package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class Review implements Serializable {
    private Long id;
    private Integer orderId;
    private Long productId;
    private Integer buyerId;
    private Integer sellerId;
    private Integer productRating; // 商品评分 1-5
    private Integer sellerRating; // 卖家评分 1-5
    private String comment; // 评价内容
    private String reviewImages; // 评价图片，多个用逗号分隔
    private Boolean isAnonymous; // 是否匿名评价
    private Instant createdAt;
    private Instant updatedAt;
}

