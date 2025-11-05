package lut.cn.c2cplatform.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lut.cn.c2cplatform.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @Autowired
    private CaptchaService captchaService;

    @Value("${captcha.expiration:300}")
    private long captchaTtlSeconds;

    @GetMapping("/generate")
    public void generateCaptcha(HttpServletResponse response) {
        String captchaId = UUID.randomUUID().toString();
        String captchaText = defaultKaptcha.createText();

        // 缓存验证码（可选 Redis / 本地内存）
        captchaService.save(captchaId, captchaText, captchaTtlSeconds);

        // 生成图片
        BufferedImage image = defaultKaptcha.createImage(captchaText);
        response.setContentType("image/jpeg");
        response.setHeader("Captcha-ID", captchaId);

        // 设置HTTP头，防止缓存
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies

        try (ServletOutputStream out = response.getOutputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            out.write(baos.toByteArray());
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha", e);
        }
    }
}
