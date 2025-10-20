package lut.cn.c2cplatform.service;

public interface CaptchaService {
    void save(String id, String code, long ttlSeconds);
    boolean verifyAndConsume(String id, String code);
}

