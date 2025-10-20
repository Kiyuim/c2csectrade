package lut.cn.c2cplatform.config;

import lut.cn.c2cplatform.service.CaptchaService;
import lut.cn.c2cplatform.service.impl.InMemoryCaptchaService;
import lut.cn.c2cplatform.service.impl.RedisCaptchaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CaptchaStoreConfig {
    @Bean
    @ConditionalOnProperty(name = "captcha.store", havingValue = "memory", matchIfMissing = true)
    public CaptchaService inMemoryCaptchaService() {
        return new InMemoryCaptchaService();
    }

    @Bean
    @ConditionalOnProperty(name = "captcha.store", havingValue = "redis")
    public CaptchaService redisCaptchaService(StringRedisTemplate template) {
        return new RedisCaptchaService(template);
    }
}

