package lut.cn.c2cplatform.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String paymentMethod;
    private String password;
}

