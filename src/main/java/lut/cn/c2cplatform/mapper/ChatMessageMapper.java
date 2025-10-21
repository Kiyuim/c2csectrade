package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    void insert(ChatMessage chatMessage);

    List<ChatMessage> findHistoryBetweenUsers(@Param("userId1") Long userId1,
                                              @Param("userId2") Long userId2,
                                              @Param("limit") int limit);

    void markAsRead(@Param("senderId") Long senderId,
                    @Param("recipientId") Long recipientId);

    int countUnreadMessages(@Param("recipientId") Long recipientId);

    List<String> findConversationPartners(@Param("userId") Long userId);

    ChatMessage findLastMessageBetweenUsers(@Param("userId1") Long userId1,
                                           @Param("userId2") Long userId2);

    int countUnreadMessagesBetweenUsers(@Param("recipientId") Long recipientId,
                                       @Param("senderId") Long senderId);
}

