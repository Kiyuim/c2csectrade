package lut.cn.c2cplatform.service.impl;

import lut.cn.c2cplatform.service.CaptchaService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCaptchaService implements CaptchaService {
    private static class Entry {
        final String code;
        final long expiresAt;
        Entry(String code, long expiresAt) { this.code = code; this.expiresAt = expiresAt; }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String id, String code, long ttlSeconds) {
        long expiresAt = System.currentTimeMillis() + ttlSeconds * 1000L;
        store.put(key(id), new Entry(code, expiresAt));
    }

    @Override
    public boolean verifyAndConsume(String id, String code) {
        String k = key(id);
        Entry e = store.get(k);
        if (e == null) return false;
        if (e.expiresAt < System.currentTimeMillis()) { store.remove(k); return false; }
        boolean ok = e.code != null && e.code.equalsIgnoreCase(code);
        if (ok) store.remove(k);
        return ok;
    }

    private String key(String id) { return id == null ? null : id; }
}

