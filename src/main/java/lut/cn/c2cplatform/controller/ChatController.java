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
            String adminUsername = authentication.getName();

            // 验证是否为管理员
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(403).body("只有管理员可以发送系统消息");
            }

            // 保存系统消息到数据库
            ChatMessage savedMessage = chatMessageService.saveMessage(
                "系统",  // 发送者显示为"系统"
                chatMessageDTO.getRecipient(),
                chatMessageDTO.getContent(),
                true  // 标记为系统消息
            );

            // 构建返回的DTO
            ChatMessageDTO responseDTO = ChatMessageDTO.builder()
                    .sender("系统")
                    .recipient(chatMessageDTO.getRecipient())
                    .content(savedMessage.getContent())
                    .timestamp(savedMessage.getTimestamp())
                    .isSystemMessage(true)
                    .build();

            // 发送给接收者
            messagingTemplate.convertAndSendToUser(
                chatMessageDTO.getRecipient(),
                "/queue/private",
                responseDTO
            );

            return ResponseEntity.ok().body("系统消息发送成功");
        } catch (Exception e) {
            System.err.println("Error sending system message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("发送失败: " + e.getMessage());
        }
    }
}
