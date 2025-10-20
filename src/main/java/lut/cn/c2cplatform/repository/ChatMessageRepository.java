package lut.cn.c2cplatform.repository;

import lut.cn.c2cplatform.entity.ChatMessage;
import lut.cn.c2cplatform.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMessageRepository {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    public void save(ChatMessage chatMessage) {
        chatMessageMapper.insert(chatMessage);
    }

    public List<ChatMessage> findHistoryBetweenUsers(Long userId1, Long userId2, int limit) {
        return chatMessageMapper.findHistoryBetweenUsers(userId1, userId2, limit);
    }

    public void markAsRead(Long senderId, Long recipientId) {
        chatMessageMapper.markAsRead(senderId, recipientId);
    }

    public int countUnreadMessages(Long recipientId) {
        return chatMessageMapper.countUnreadMessages(recipientId);
    }
}

