package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.entity.ChatMessage;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserMapper userMapper;

    public ChatMessage saveMessage(String senderUsername, String recipientUsername, String content) {
        User sender = userMapper.selectByUsername(senderUsername);
        User recipient = userMapper.selectByUsername(recipientUsername);

        if (sender == null || recipient == null) {
            throw new RuntimeException("User not found");
        }

        ChatMessage message = ChatMessage.builder()
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .content(content)
                .timestamp(new Date())
                .isRead(false)
                .senderUsername(senderUsername)
                .recipientUsername(recipientUsername)
                .build();

        chatMessageRepository.save(message);
        return message;
    }

    public List<ChatMessage> getChatHistory(String username1, String username2, int limit) {
        User user1 = userMapper.selectByUsername(username1);
        User user2 = userMapper.selectByUsername(username2);

        if (user1 == null || user2 == null) {
            throw new RuntimeException("User not found");
        }

        return chatMessageRepository.findHistoryBetweenUsers(user1.getId(), user2.getId(), limit);
    }

    public void markMessagesAsRead(String senderUsername, String recipientUsername) {
        User sender = userMapper.selectByUsername(senderUsername);
        User recipient = userMapper.selectByUsername(recipientUsername);

        if (sender == null || recipient == null) {
            throw new RuntimeException("User not found");
        }

        chatMessageRepository.markAsRead(sender.getId(), recipient.getId());
    }

    public int getUnreadCount(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return chatMessageRepository.countUnreadMessages(user.getId());
    }
}
