package lut.cn.c2cplatform.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
    private String username;
    private String email;
    private String newPassword;
    private String captchaId;
    private String captchaCode;
}

