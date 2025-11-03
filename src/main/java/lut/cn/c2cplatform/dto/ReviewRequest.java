package lut.cn.c2cplatform.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {
    private Integer orderId;
    private Integer productRating; // 1-5
    private Integer sellerRating; // 1-5
    private String comment;
    private List<String> reviewImages;
    private Boolean isAnonymous;
}

