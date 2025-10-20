package lut.cn.c2cplatform.payload.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String captchaId;
    private String captchaCode;
}

