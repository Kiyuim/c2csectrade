package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.event.Events;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Real-time Recommendation Service
 * Simulates stream processing for immediate recommendation updates
 *
 * In production, use:
 * - Apache Flink / Spark Streaming for real-time processing
 * - Kafka for event streaming
 * - Time window aggregations
 */
@Service
public class RealtimeRecommendationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NCFRecommendationService ncfService;

    @Autowired
    private HybridRecommendationService hybridService;

    // In-memory stream buffer (in production, use Kafka/Flink)
    private final Map<String, Queue<Events.RecommendationUpdateEvent>> eventStreams = new ConcurrentHashMap<>();

    private static final String REALTIME_SCORE_PREFIX = "realtime:score:";
    private static final String TRENDING_KEY = "realtime:trending";
    private static final String USER_SESSION_PREFIX = "realtime:session:";

    // Time window for real-time aggregation (5 minutes)
    private static final long TIME_WINDOW_SECONDS = 300;

    /**
     * Process real-time user interaction
     * Updates recommendation scores immediately
     */
    public void processInteraction(Long userId, Long productId, String action) {
        // 1. Update trending scores
        updateTrendingScore(productId, action);

        // 2. Update user session data
        updateUserSession(userId, productId, action);

        // 3. Train NCF model in real-time
        ncfService.trainOnInteraction(userId, productId, action);

        // 4. Update popularity score
        hybridService.updatePopularity(productId, action);

        // 5. Compute real-time personalized score
        computeRealtimeScore(userId, productId);
    }

    /**
     * Get trending products in last N minutes
     */
    public List<Long> getTrendingProducts(int minutes, int limit) {
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (minutes * 60 * 1000);

        // Get products with scores in time window
        Set<Object> trending = redisTemplate.opsForZSet()
            .reverseRangeByScore(TRENDING_KEY, startTime, currentTime, 0, limit);

        if (trending == null || trending.isEmpty()) {
            return Collections.emptyList();
        }

        return trending.stream()
            .map(obj -> {
                String str = obj.toString();
                return Long.parseLong(str.split(":")[0]);
            })
            .distinct()
            .limit(limit)
            .toList();
    }

    /**
     * Get real-time personalized recommendations
     * Combines session data, NCF, and trending
     */
    public List<Long> getRealtimeRecommendations(Long userId, int limit) {
        Map<Long, Double> scores = new HashMap<>();

        // 1. Get user's current session products
        List<Long> sessionProducts = getUserSessionProducts(userId);

        // 2. Get trending products
        List<Long> trending = getTrendingProducts(30, 50);

        // 3. Combine and score
        Set<Long> candidates = new HashSet<>();
        candidates.addAll(trending);

        // Get similar products for session items
        for (Long productId : sessionProducts) {
            List<Long> similar = hybridService.getHybridSimilarProducts(productId, 10);
            candidates.addAll(similar);
        }

        // 4. Score using NCF
        for (Long productId : candidates) {
            if (!sessionProducts.contains(productId)) {
                double ncfScore = ncfService.predictScore(userId, productId);
                double trendingScore = getTrendingScore(productId);
                double sessionScore = getSessionRelevanceScore(userId, productId);

                // Weighted combination
                double finalScore = 0.5 * ncfScore + 0.3 * trendingScore + 0.2 * sessionScore;
                scores.put(productId, finalScore);
            }
        }

        return scores.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Update trending score with time decay
     */
    private void updateTrendingScore(Long productId, String action) {
        double weight = getActionWeight(action);
        long timestamp = System.currentTimeMillis();

        String member = productId + ":" + timestamp;
        redisTemplate.opsForZSet().add(TRENDING_KEY, member, timestamp);

        // Clean up old entries (older than 1 hour)
        long oldestAllowed = timestamp - (3600 * 1000);
        redisTemplate.opsForZSet().removeRangeByScore(TRENDING_KEY, 0, oldestAllowed);
    }

    /**
     * Update user session data
     */
    private void updateUserSession(Long userId, Long productId, String action) {
        String key = USER_SESSION_PREFIX + userId;

        // Add to session with timestamp
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("productId", productId);
        sessionData.put("action", action);
        sessionData.put("timestamp", System.currentTimeMillis());

        redisTemplate.opsForList().leftPush(key, sessionData);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        // Keep only last 20 interactions
        redisTemplate.opsForList().trim(key, 0, 19);
    }

    /**
     * Get user's session products
     */
    private List<Long> getUserSessionProducts(Long userId) {
        String key = USER_SESSION_PREFIX + userId;
        List<Object> sessionData = redisTemplate.opsForList().range(key, 0, 19);

        if (sessionData == null || sessionData.isEmpty()) {
            return Collections.emptyList();
        }

        return sessionData.stream()
            .filter(obj -> obj instanceof Map)
            .map(obj -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) obj;
                Object productId = data.get("productId");
                return productId instanceof Long ? (Long) productId : null;
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    /**
     * Compute real-time personalized score
     */
    private void computeRealtimeScore(Long userId, Long productId) {
        String key = REALTIME_SCORE_PREFIX + userId + ":" + productId;

        // Compute score based on multiple signals
        double ncfScore = ncfService.predictScore(userId, productId);
        double trendingScore = getTrendingScore(productId);

        double realtimeScore = 0.7 * ncfScore + 0.3 * trendingScore;

        redisTemplate.opsForValue().set(key, realtimeScore, 10, TimeUnit.MINUTES);
    }

    /**
     * Get trending score for a product
     */
    public double getTrendingScore(Long productId) {
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (TIME_WINDOW_SECONDS * 1000);

        Set<Object> scores = redisTemplate.opsForZSet()
            .rangeByScore(TRENDING_KEY, startTime, currentTime);

        if (scores == null) return 0.0;

        long count = scores.stream()
            .filter(obj -> obj.toString().startsWith(productId + ":"))
            .count();

        // Normalize by time window
        return Math.min(1.0, count / 100.0);
    }

    /**
     * Get session relevance score
     */
    private double getSessionRelevanceScore(Long userId, Long productId) {
        List<Long> sessionProducts = getUserSessionProducts(userId);

        if (sessionProducts.isEmpty()) return 0.0;

        // Check if product is similar to session products
        int similarCount = 0;
        for (Long sessionProduct : sessionProducts) {
            List<Long> similar = hybridService.getHybridSimilarProducts(sessionProduct, 20);
            if (similar.contains(productId)) {
                similarCount++;
            }
        }

        return Math.min(1.0, similarCount / (double) sessionProducts.size());
    }

    private double getActionWeight(String action) {
        return switch (action.toLowerCase()) {
            case "view" -> 1.0;
            case "favorite" -> 3.0;
            case "cart" -> 5.0;
            case "order" -> 10.0;
            default -> 0.5;
        };
    }

    /**
     * Window aggregation (simulate Flink window)
     */
    public Map<String, Long> aggregateWindow(int windowMinutes) {
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (windowMinutes * 60 * 1000);

        Set<Object> events = redisTemplate.opsForZSet()
            .rangeByScore(TRENDING_KEY, startTime, currentTime);

        Map<String, Long> aggregation = new HashMap<>();

        if (events != null) {
            for (Object event : events) {
                String[] parts = event.toString().split(":");
                if (parts.length > 0) {
                    aggregation.merge(parts[0], 1L, Long::sum);
                }
            }
        }

        return aggregation;
    }
}

