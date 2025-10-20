package lut.cn.c2cplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String senderUsername;
    private String recipientUsername;
    private String content;
    private Date timestamp;
    private Boolean isRead;
}
