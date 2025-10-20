package lut.cn.c2cplatform.payload;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    // 是否以管理员身份登录（前端勾选"管理员登录"时为 true）
    private Boolean isAdmin;
    private String captchaId;
    private String captchaCode;
}
