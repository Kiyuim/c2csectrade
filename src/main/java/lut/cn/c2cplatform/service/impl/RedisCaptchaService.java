package lut.cn.c2cplatform.service.impl;

import lut.cn.c2cplatform.service.CaptchaService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisCaptchaService implements CaptchaService {
    private final StringRedisTemplate redisTemplate;
    public RedisCaptchaService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }

    @Override
    public void save(String id, String code, long ttlSeconds) {
        if (id == null || code == null) return;
        redisTemplate.opsForValue().set(key(id), code, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean verifyAndConsume(String id, String code) {
        if (id == null || code == null) return false;
        String k = key(id);
        String val = redisTemplate.opsForValue().get(k);
        if (val != null && val.equalsIgnoreCase(code)) {
            redisTemplate.delete(k);
            return true;
        }
        return false;
    }

    private String key(String id) { return "captcha:" + id; }
}

