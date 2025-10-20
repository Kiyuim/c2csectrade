package lut.cn.c2cplatform.payload.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String captchaId;
    private String captchaCode;
    private Boolean isAdmin;
}

