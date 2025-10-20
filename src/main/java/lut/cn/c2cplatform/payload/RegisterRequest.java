package lut.cn.c2cplatform.payload;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String captchaId;
    private String captchaCode;
}
