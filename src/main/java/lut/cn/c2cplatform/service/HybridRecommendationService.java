package lut.cn.c2cplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid Recommendation Service
 * Combines multiple recommendation strategies:
 * 1. Collaborative Filtering (user behavior)
 * 2. Content-Based (product features)
 * 3. Popularity-Based (trending items)
 */
@Service
public class HybridRecommendationService {

    @Autowired
    private RecommendationEngineService collaborativeFiltering;

    @Autowired
    private ContentBasedRecommendationService contentBased;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String POPULARITY_KEY = "recommend:popularity";
    private static final double CF_WEIGHT = 0.4;      // 协同过滤权重
    private static final double CONTENT_WEIGHT = 0.4; // 内容相似度权重
    private static final double POPULARITY_WEIGHT = 0.2; // 热度权重

    /**
     * Get hybrid recommendations for a product
     * Combines CF, content-based, and popularity signals
     */
    public List<Long> getHybridSimilarProducts(Long productId, int limit) {
        Map<Long, Double> combinedScores = new HashMap<>();

        // 1. Get collaborative filtering recommendations
        List<Long> cfRecommendations = collaborativeFiltering.getSimilarProducts(productId, limit * 2);
        for (int i = 0; i < cfRecommendations.size(); i++) {
            Long recId = cfRecommendations.get(i);
            // Score decreases with rank
            double score = (cfRecommendations.size() - i) / (double) cfRecommendations.size();
            combinedScores.merge(recId, score * CF_WEIGHT, Double::sum);
        }

        // 2. Get content-based recommendations
        List<Long> contentRecommendations = contentBased.getContentBasedSimilar(productId, limit * 2);
        for (int i = 0; i < contentRecommendations.size(); i++) {
            Long recId = contentRecommendations.get(i);
            double score = (contentRecommendations.size() - i) / (double) contentRecommendations.size();
            combinedScores.merge(recId, score * CONTENT_WEIGHT, Double::sum);
        }

        // 3. Add popularity boost
        addPopularityBoost(combinedScores);

        // Sort by combined score and return top N
        return combinedScores.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Add popularity boost to scores
     */
    private void addPopularityBoost(Map<Long, Double> scores) {
        try {
            for (Long productId : scores.keySet()) {
                Double popularity = getPopularityScore(productId);
                if (popularity != null && popularity > 0) {
                    scores.merge(productId, popularity * POPULARITY_WEIGHT, Double::sum);
                }
            }
        } catch (Exception e) {
            // Silent fail - popularity is optional
            System.err.println("Failed to add popularity boost: " + e.getMessage());
        }
    }

    /**
     * Get popularity score for a product
     * Based on views, favorites, orders, etc.
     */
    private Double getPopularityScore(Long productId) {
        try {
            Double score = redisTemplate.opsForZSet().score(POPULARITY_KEY, productId.toString());
            return score != null ? score : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Update product popularity score
     * Should be called when users interact with products
     */
    public void updatePopularity(Long productId, String action) {
        try {
            double increment = getActionWeight(action);
            redisTemplate.opsForZSet().incrementScore(POPULARITY_KEY, productId.toString(), increment);
        } catch (Exception e) {
            System.err.println("Failed to update popularity: " + e.getMessage());
        }
    }

    /**
     * Get weight for different user actions
     */
    private double getActionWeight(String action) {
        switch (action.toLowerCase()) {
            case "view":
                return 1.0;
            case "favorite":
                return 3.0;
            case "cart":
                return 5.0;
            case "order":
                return 10.0;
            case "review":
                return 8.0;
            default:
                return 1.0;
        }
    }

    /**
     * Get personalized recommendations for user
     * Uses user's interest profile and hybrid approach
     */
    public Map<Long, Double> getPersonalizedScores(Map<String, Integer> userInterests, Set<Long> viewedProducts, int limit) {
        Map<Long, Double> scores = new HashMap<>();

        // For each interest, get recommendations and combine scores
        for (Map.Entry<String, Integer> interest : userInterests.entrySet()) {
            String tag = interest.getKey();
            Integer interestScore = interest.getValue();

            // Get products with this tag and boost by user interest
            // This would typically query from database or search engine
            // For now, we'll use a simplified approach
            double boost = Math.log(interestScore + 1) / Math.log(10); // Logarithmic scaling
            // Implementation depends on how tags are stored
        }

        return scores;
    }

    /**
     * Decay old popularity scores (scheduled maintenance)
     * Prevents old popular items from dominating forever
     */
    public void decayPopularityScores() {
        try {
            Set<Object> allProducts = redisTemplate.opsForZSet().range(POPULARITY_KEY, 0, -1);
            if (allProducts == null || allProducts.isEmpty()) {
                return;
            }

            // Decay by 10%
            double decayFactor = 0.9;
            for (Object productId : allProducts) {
                Double currentScore = redisTemplate.opsForZSet().score(POPULARITY_KEY, productId.toString());
                if (currentScore != null && currentScore > 0) {
                    double newScore = currentScore * decayFactor;
                    redisTemplate.opsForZSet().add(POPULARITY_KEY, productId.toString(), newScore);
                }
            }

            System.out.println("Decayed popularity scores for " + allProducts.size() + " products");
        } catch (Exception e) {
            System.err.println("Failed to decay popularity scores: " + e.getMessage());
        }
    }
}

