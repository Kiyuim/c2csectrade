package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.dto.ViewHistoryDTO;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tracking user browsing history and interest profiles
 * Uses Redis for high-performance real-time data capture
 */
@Service
public class HistoryService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    private static final String HISTORY_KEY_PREFIX = "history:user:";
    private static final String PROFILE_KEY_PREFIX = "profile:user:";
    private static final int MAX_HISTORY_SIZE = 100; // Keep last 100 views per user

    /**
     * Record a product view by a user
     * Uses Redis Sorted Set with timestamp as score for automatic sorting and deduplication
     */
    public void recordView(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return;
        }

        String historyKey = HISTORY_KEY_PREFIX + userId;
        double timestamp = System.currentTimeMillis();

        // Add to user's browsing history (Sorted Set)
        redisTemplate.opsForZSet().add(historyKey, productId.toString(), timestamp);

        // Trim to keep only last MAX_HISTORY_SIZE items
        Long size = redisTemplate.opsForZSet().zCard(historyKey);
        if (size != null && size > MAX_HISTORY_SIZE) {
            // Remove oldest items (lowest scores)
            redisTemplate.opsForZSet().removeRange(historyKey, 0, size - MAX_HISTORY_SIZE - 1);
        }

        // Update user interest profile based on product tags
        updateUserInterestProfile(userId, productId);
    }

    /**
     * Update user's interest profile based on viewed product
     * Uses Redis Hash to maintain tag interest scores
     */
    private void updateUserInterestProfile(Long userId, Long productId) {
        try {
            // Get product details to extract tags/category
            Product product = productMapper.selectById(productId);
            if (product == null) {
                return;
            }

            String profileKey = PROFILE_KEY_PREFIX + userId;

            // Extract tags from category (you can extend this to use actual tags field)
            List<String> tags = extractTags(product);

            // Increment interest score for each tag
            for (String tag : tags) {
                redisTemplate.opsForHash().increment(profileKey, tag, 1);
            }
        } catch (Exception e) {
            // Log but don't fail the main operation
            System.err.println("Failed to update user interest profile: " + e.getMessage());
        }
    }

    /**
     * Extract tags from product (category-based)
     * TODO: Extend to support explicit tags field if added to Product entity
     */
    private List<String> extractTags(Product product) {
        List<String> tags = new ArrayList<>();

        if (product.getCategory() != null && !product.getCategory().trim().isEmpty()) {
            // Add category as a tag
            tags.add(product.getCategory());

            // If category contains subcategories (e.g., "Books > Programming")
            String[] parts = product.getCategory().split(">");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !tags.contains(trimmed)) {
                    tags.add(trimmed);
                }
            }
        }

        // Add condition level as a tag
        if (product.getConditionLevel() != null) {
            tags.add("condition_" + product.getConditionLevel());
        }

        return tags;
    }

    /**
     * Get user's browsing history (most recent first)
     */
    public List<ViewHistoryDTO> getUserHistory(Long userId, int limit) {
        String historyKey = HISTORY_KEY_PREFIX + userId;

        // Get latest items (highest scores)
        Set<ZSetOperations.TypedTuple<Object>> items =
            redisTemplate.opsForZSet().reverseRangeWithScores(historyKey, 0, limit - 1);

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
            .map(item -> new ViewHistoryDTO(
                Long.parseLong(item.getValue().toString()),
                item.getScore().longValue()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get user's interest profile (sorted by interest score)
     */
    public Map<String, Integer> getUserInterestProfile(Long userId, int topN) {
        String profileKey = PROFILE_KEY_PREFIX + userId;

        Map<Object, Object> allInterests = redisTemplate.opsForHash().entries(profileKey);

        if (allInterests.isEmpty()) {
            return Collections.emptyMap();
        }

        // Sort by score descending and take top N
        return allInterests.entrySet().stream()
            .sorted((e1, e2) -> {
                Integer score1 = Integer.parseInt(e1.getValue().toString());
                Integer score2 = Integer.parseInt(e2.getValue().toString());
                return score2.compareTo(score1);
            })
            .limit(topN)
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Integer.parseInt(e.getValue().toString()),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * Get all users who have browsing history (for batch processing)
     */
    public Set<String> getAllUsersWithHistory() {
        Set<String> keys = redisTemplate.keys(HISTORY_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }

        return keys.stream()
            .map(key -> key.replace(HISTORY_KEY_PREFIX, ""))
            .collect(Collectors.toSet());
    }

    /**
     * Get product IDs from user's history (for recommendation computation)
     */
    public Set<Long> getUserViewedProductIds(Long userId) {
        String historyKey = HISTORY_KEY_PREFIX + userId;
        Set<Object> items = redisTemplate.opsForZSet().range(historyKey, 0, -1);

        if (items == null || items.isEmpty()) {
            return Collections.emptySet();
        }

        return items.stream()
            .map(obj -> Long.parseLong(obj.toString()))
            .collect(Collectors.toSet());
    }
}

