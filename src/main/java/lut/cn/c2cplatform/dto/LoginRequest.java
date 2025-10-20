package lut.cn.c2cplatform.dto;

import lombok.Data;
@Data public class LoginRequest {
    private String username;
    private String password;
    private String captchaId;
    private String captchaCode;
}
