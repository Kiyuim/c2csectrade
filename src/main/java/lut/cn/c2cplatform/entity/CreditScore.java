package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class CreditScore implements Serializable {
    private Long id;
    private Long userId;
    private Integer totalScore; // 总信用分
    private Integer level; // 信用等级 1-5
    private Integer totalSales; // 总销售数
    private Integer totalPurchases; // 总购买数
    private Double averageSellerRating; // 平均卖家评分
    private Integer positiveReviews; // 好评数
    private Integer neutralReviews; // 中评数
    private Integer negativeReviews; // 差评数
    private Instant updatedAt;
}

