package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.payload.RegisterRequest;
import lut.cn.c2cplatform.payload.LoginRequest;
import lut.cn.c2cplatform.payload.AuthResponse;
import lut.cn.c2cplatform.service.AuthService;
import lut.cn.c2cplatform.service.CaptchaService;
import lut.cn.c2cplatform.security.JwtTokenProvider;
import lut.cn.c2cplatform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (!captchaService.verifyAndConsume(registerRequest.getCaptchaId(), registerRequest.getCaptchaCode())) {
            return ResponseEntity.badRequest().body("验证码错误或已过期");
        }

        User user = authService.registerUser(registerRequest);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (!captchaService.verifyAndConsume(loginRequest.getCaptchaId(), loginRequest.getCaptchaCode())) {
            return ResponseEntity.badRequest().body("验证码错误或已过期");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean wantAdmin = Boolean.TRUE.equals(loginRequest.getIsAdmin());
        if (wantAdmin && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("需要管理员权限，请取消勾选或使用管理员账号。");
        }

        String token = jwtTokenProvider.generateToken(authentication);
        String role = authentication.getAuthorities().stream().findFirst().map(Object::toString).orElse("");
        return ResponseEntity.ok(new AuthResponse(token, loginRequest.getUsername(), role));
    }
}
