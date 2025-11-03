package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ChatMessageDTO;
import lut.cn.c2cplatform.dto.ConversationDTO;
import lut.cn.c2cplatform.entity.ChatMessage;
import lut.cn.c2cplatform.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageService chatMessageService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO chatMessageDTO, Principal principal) {
        try {
            // 使用认证的用户名作为发送者
            String senderUsername = principal.getName();

            // 保存消息到数据库 (普通消息不是系统消息)
            ChatMessage savedMessage = chatMessageService.saveMessage(
                senderUsername,
                chatMessageDTO.getRecipient(),
                chatMessageDTO.getContent(),
                false
            );

            // 构建返回的DTO
            ChatMessageDTO responseDTO = ChatMessageDTO.builder()
                    .sender(senderUsername)
                    .recipient(chatMessageDTO.getRecipient())
                    .content(savedMessage.getContent())
                    .timestamp(savedMessage.getTimestamp())
                    .isSystemMessage(false)
                    .build();

            // 发送给接收者
            messagingTemplate.convertAndSendToUser(
                chatMessageDTO.getRecipient(),
                "/queue/private",
                responseDTO
            );

            // 也发送给发送者（用于多设备同步）
            messagingTemplate.convertAndSendToUser(
                senderUsername,
                "/queue/private",
                responseDTO
            );

        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // REST API：获取聊天历史
    @GetMapping("/api/chat/history/{username}")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable String username,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {

        String currentUsername = authentication.getName();

        List<ChatMessage> messages = chatMessageService.getChatHistory(currentUsername, username, limit);

        List<ChatMessageDTO> dtos = messages.stream()
                .map(msg -> ChatMessageDTO.builder()
                        .sender(msg.getSenderUsername())
                        .recipient(msg.getRecipientUsername())
                        .content(msg.getContent())
                        .timestamp(msg.getTimestamp())
                        .isSystemMessage(msg.getIsSystemMessage() != null ? msg.getIsSystemMessage() : false)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // REST API：标记消息为已读
    @PostMapping("/api/chat/read/{username}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String username,
            Authentication authentication) {

        String currentUsername = authentication.getName();
        chatMessageService.markMessagesAsRead(username, currentUsername);

        return ResponseEntity.ok().build();
    }

    // REST API：获取未读消息数
    @GetMapping("/api/chat/unread-count")
    public ResponseEntity<Integer> getUnreadCount(Authentication authentication) {
        String currentUsername = authentication.getName();
        int count = chatMessageService.getUnreadCount(currentUsername);
        return ResponseEntity.ok(count);
    }

    // REST API：获取会话列表
    @GetMapping("/api/chat/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(Authentication authentication) {
        String currentUsername = authentication.getName();
        List<ConversationDTO> conversations = chatMessageService.getConversations(currentUsername);
        return ResponseEntity.ok(conversations);
    }

    // REST API：管理员发送系统消息
    @PostMapping("/api/chat/admin/send-system-message")
    public ResponseEntity<?> sendSystemMessage(
            @RequestBody ChatMessageDTO chatMessageDTO,
            Authentication authentication) {
        try {
            System.out.println("[SYSTEM_MSG] 开始发送系统消息到: " + chatMessageDTO.getRecipient());
            String adminUsername = authentication.getName();

            // 验证是否为管理员
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                System.err.println("[SYSTEM_MSG] 权限不足: " + adminUsername);
                return ResponseEntity.status(403).body("只有管理员可以发送系统消息");
            }

            // 保存系统消息到数据库
            ChatMessage savedMessage = chatMessageService.saveMessage(
                "系统",  // 发送者显示为"系统"
                chatMessageDTO.getRecipient(),
                chatMessageDTO.getContent(),
                true  // 标记为系统消息
            );
            System.out.println("[SYSTEM_MSG] 消息已保存到数据库，ID: " + savedMessage.getId());

            // 构建返回的DTO
            ChatMessageDTO responseDTO = ChatMessageDTO.builder()
                    .sender("系统")
                    .recipient(chatMessageDTO.getRecipient())
                    .content(savedMessage.getContent())
                    .timestamp(savedMessage.getTimestamp())
                    .isSystemMessage(true)
                    .build();

            // 发送给接收者
            System.out.println("[SYSTEM_MSG] 尝试通过WebSocket发送到用户: " + chatMessageDTO.getRecipient());
            messagingTemplate.convertAndSendToUser(
                chatMessageDTO.getRecipient(),
                "/queue/private",
                responseDTO
            );
            System.out.println("[SYSTEM_MSG] WebSocket发送完成");

            return ResponseEntity.ok().body("系统消息发送成功");
        } catch (Exception e) {
            System.err.println("[SYSTEM_MSG] 发送失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("发送失败: " + e.getMessage());
        }
    }

    // REST API：管理员群发系统消息给所有用户
    @PostMapping("/api/message/broadcast")
    public ResponseEntity<?> broadcastSystemMessage(
            @RequestBody java.util.Map<String, String> payload,
            Authentication authentication) {
        try {
            System.out.println("[BROADCAST] 开始群发系统消息");

            // 验证是否为管理员
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                System.err.println("[BROADCAST] 权限不足");
                return ResponseEntity.status(403).body("只有管理员可以发送系统消息");
            }

            String message = payload.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("消息内容不能为空");
            }

            // 获取所有用户
            String adminUsername = authentication.getName();
            List<lut.cn.c2cplatform.entity.User> allUsers = chatMessageService.getAllUsers();
            System.out.println("[BROADCAST] 找到 " + allUsers.size() + " 个用户");

            int successCount = 0;
            int failCount = 0;

            for (lut.cn.c2cplatform.entity.User user : allUsers) {
                // 跳过管理员自己
                if (user.getUsername().equals(adminUsername)) {
                    System.out.println("[BROADCAST] 跳过管理员: " + user.getUsername());
                    continue;
                }

                try {
                    System.out.println("[BROADCAST] 发送给用户: " + user.getUsername());

                    // 保存系统消息到数据库
                    ChatMessage savedMessage = chatMessageService.saveMessage(
                        "系统",
                        user.getUsername(),
                        message,
                        true
                    );

                    // 构建返回的DTO
                    ChatMessageDTO responseDTO = ChatMessageDTO.builder()
                            .sender("系统")
                            .recipient(user.getUsername())
                            .content(savedMessage.getContent())
                            .timestamp(savedMessage.getTimestamp())
                            .isSystemMessage(true)
                            .build();

                    // 发送给用户
                    messagingTemplate.convertAndSendToUser(
                        user.getUsername(),
                        "/queue/private",
                        responseDTO
                    );

                    successCount++;
                } catch (Exception e) {
                    System.err.println("[BROADCAST] 发送给 " + user.getUsername() + " 失败: " + e.getMessage());
                    e.printStackTrace();
                    failCount++;
                }
            }

            String result = String.format("群发完成：成功 %d，失败 %d", successCount, failCount);
            System.out.println("[BROADCAST] " + result);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            System.err.println("[BROADCAST] 群发失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("群发失败: " + e.getMessage());
        }
    }
}
