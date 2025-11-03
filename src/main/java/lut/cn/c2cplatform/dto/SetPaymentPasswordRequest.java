package lut.cn.c2cplatform.dto;

import lombok.Data;

@Data
public class SetPaymentPasswordRequest {
    private String password;
    private String confirmPassword;
}

