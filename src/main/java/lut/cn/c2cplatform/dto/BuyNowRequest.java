package lut.cn.c2cplatform.dto;

import lombok.Data;

@Data
public class BuyNowRequest {
    private Long productId;
    private Integer quantity;
}

