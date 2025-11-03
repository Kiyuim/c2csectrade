package lut.cn.c2cplatform.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ReviewResponse {
    private Long id;
    private Integer orderId;
    private Long productId;
    private String productName;
    private Integer buyerId;
    private String buyerName;
    private String buyerAvatar;
    private Integer sellerId;
    private Integer productRating;
    private Integer sellerRating;
    private String comment;
    private List<String> reviewImages;
    private Boolean isAnonymous;
    private Instant createdAt;
}

