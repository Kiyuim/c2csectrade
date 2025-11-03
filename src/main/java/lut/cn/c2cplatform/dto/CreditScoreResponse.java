package lut.cn.c2cplatform.dto;

import lombok.Data;

@Data
public class CreditScoreResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Integer totalScore;
    private Integer level;
    private String levelName; // 信用等级名称
    private Integer totalSales;
    private Integer totalPurchases;
    private Double averageSellerRating;
    private Integer positiveReviews;
    private Integer neutralReviews;
    private Integer negativeReviews;
    private Integer totalReviews;
    private Double positiveRate; // 好评率
}

