package lut.cn.c2cplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Advanced Redis Cache Service
 * Provides multi-level caching, distributed locking, and cache warming
 */
@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Cache key prefixes
    private static final String PRODUCT_CACHE_PREFIX = "cache:product:";
    private static final String USER_CACHE_PREFIX = "cache:user:";
    private static final String HOT_PRODUCTS_KEY = "cache:hot:products";
    private static final String LOCK_PREFIX = "lock:";

    /**
     * Cache product details with TTL
     */
    public void cacheProduct(Long productId, Object product, long ttlMinutes) {
        String key = PRODUCT_CACHE_PREFIX + productId;
        redisTemplate.opsForValue().set(key, product, ttlMinutes, TimeUnit.MINUTES);
    }

    /**
     * Get cached product
     */
    public Object getCachedProduct(Long productId) {
        String key = PRODUCT_CACHE_PREFIX + productId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Cache user profile
     */
    public void cacheUser(Long userId, Object user, long ttlMinutes) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, user, ttlMinutes, TimeUnit.MINUTES);
    }

    /**
     * Get cached user
     */
    public Object getCachedUser(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Distributed lock implementation
     * Returns true if lock acquired, false otherwise
     */
    public boolean acquireLock(String resource, String lockId, long expireSeconds) {
        String key = LOCK_PREFIX + resource;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, lockId, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Release distributed lock
     */
    public boolean releaseLock(String resource, String lockId) {
        String key = LOCK_PREFIX + resource;
        Object currentValue = redisTemplate.opsForValue().get(key);

        // Only release if the lock is held by this lockId
        if (lockId.equals(currentValue)) {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        }
        return false;
    }

    /**
     * Cache hot products (top selling/viewed)
     * Uses Sorted Set for ranking
     */
    public void updateHotProducts(Long productId, double score) {
        redisTemplate.opsForZSet().add(HOT_PRODUCTS_KEY, productId.toString(), score);
    }

    /**
     * Get hot products
     */
    public List<Long> getHotProducts(int limit) {
        Set<Object> hotIds = redisTemplate.opsForZSet().reverseRange(HOT_PRODUCTS_KEY, 0, limit - 1);
        if (hotIds == null || hotIds.isEmpty()) {
            return Collections.emptyList();
        }

        return hotIds.stream()
            .map(obj -> Long.parseLong(obj.toString()))
            .toList();
    }

    /**
     * Multi-get for batch retrieval
     */
    public List<Object> multiGet(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * Pipeline operation for batch updates
     */
    public void batchSet(Map<String, Object> keyValues, long ttlMinutes) {
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            keyValues.forEach((key, value) -> {
                redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
            });
            return null;
        });
    }

    /**
     * Increment counter atomically
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * Increment with expiration
     */
    public Long incrementWithExpire(String key, long expireSeconds) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1) {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }
        return value;
    }

    /**
     * Check if key exists
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Delete cache entries
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Delete multiple keys
     */
    public void deleteMultiple(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * Get all keys matching pattern
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * Bloom filter simulation for deduplication
     * Uses bitmap operations
     */
    public void bloomAdd(String bloomKey, String value) {
        // Simple hash-based bloom filter
        int hash1 = value.hashCode() % 10000;
        int hash2 = (value.hashCode() * 31) % 10000;
        int hash3 = (value.hashCode() * 37) % 10000;

        redisTemplate.opsForValue().setBit(bloomKey, hash1, true);
        redisTemplate.opsForValue().setBit(bloomKey, hash2, true);
        redisTemplate.opsForValue().setBit(bloomKey, hash3, true);
    }

    /**
     * Check if value exists in bloom filter
     */
    public boolean bloomContains(String bloomKey, String value) {
        int hash1 = value.hashCode() % 10000;
        int hash2 = (value.hashCode() * 31) % 10000;
        int hash3 = (value.hashCode() * 37) % 10000;

        Boolean bit1 = redisTemplate.opsForValue().getBit(bloomKey, hash1);
        Boolean bit2 = redisTemplate.opsForValue().getBit(bloomKey, hash2);
        Boolean bit3 = redisTemplate.opsForValue().getBit(bloomKey, hash3);

        return Boolean.TRUE.equals(bit1) && Boolean.TRUE.equals(bit2) && Boolean.TRUE.equals(bit3);
    }
}

