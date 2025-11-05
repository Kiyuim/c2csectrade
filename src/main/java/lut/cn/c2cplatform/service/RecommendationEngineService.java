package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation Engine Service
 * Implements Item-Based Collaborative Filtering (离线计算)
 * Scheduled to run daily to compute product similarities
 */
@Service
public class RecommendationEngineService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Lazy
    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProductMapper productMapper;

    @Lazy
    @Autowired
    private ContentBasedRecommendationService contentBasedService;

    @Lazy
    @Autowired
    private HybridRecommendationService hybridService;

    private static final String RECOMMEND_KEY_PREFIX = "recommend:item-cf:";
    private static final int TOP_N_SIMILAR = 10; // Store top 10 similar products

    /**
     * Scheduled task to compute product recommendations
     * Runs daily at 3 AM (cron: second minute hour day month weekday)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void computeRecommendations() {
        System.out.println("=== Starting recommendation computation at " + new Date() + " ===");

        try {
            long startTime = System.currentTimeMillis();

            // Step 1: Build co-occurrence matrix
            Map<Long, Map<Long, Integer>> coOccurrenceMatrix = buildCoOccurrenceMatrix();

            if (coOccurrenceMatrix.isEmpty()) {
                System.out.println("No browsing data available for recommendations");
                return;
            }

            // Step 2: Compute similarity scores
            Map<Long, Map<Long, Double>> similarityMatrix = computeSimilarityMatrix(coOccurrenceMatrix);

            // Step 3: Store recommendations in Redis
            storeRecommendations(similarityMatrix);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("=== Collaborative Filtering computation completed in " + duration + "ms ===");
            System.out.println("Processed " + coOccurrenceMatrix.size() + " products");

            // Step 4: Compute content-based similarity
            System.out.println("=== Starting content-based similarity computation ===");
            contentBasedService.computeContentBasedSimilarity();

            // Step 5: Decay popularity scores
            System.out.println("=== Decaying popularity scores ===");
            hybridService.decayPopularityScores();

            System.out.println("=== All recommendation computations completed ===");

        } catch (Exception e) {
            System.err.println("Failed to compute recommendations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build co-occurrence matrix from user browsing history
     * If users viewed both product A and B, increment co-occurrence count
     */
    private Map<Long, Map<Long, Integer>> buildCoOccurrenceMatrix() {
        Map<Long, Map<Long, Integer>> matrix = new HashMap<>();

        // Get all users with browsing history
        Set<String> userIds = historyService.getAllUsersWithHistory();
        System.out.println("Processing browsing history for " + userIds.size() + " users");

        for (String userIdStr : userIds) {
            try {
                Long userId = Long.parseLong(userIdStr);
                Set<Long> viewedProducts = historyService.getUserViewedProductIds(userId);

                // For each pair of products this user viewed
                List<Long> productList = new ArrayList<>(viewedProducts);
                for (int i = 0; i < productList.size(); i++) {
                    Long product1 = productList.get(i);

                    for (int j = i + 1; j < productList.size(); j++) {
                        Long product2 = productList.get(j);

                        // Increment co-occurrence for both directions
                        incrementCoOccurrence(matrix, product1, product2);
                        incrementCoOccurrence(matrix, product2, product1);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing user " + userIdStr + ": " + e.getMessage());
            }
        }

        return matrix;
    }

    /**
     * Increment co-occurrence count between two products
     */
    private void incrementCoOccurrence(Map<Long, Map<Long, Integer>> matrix, Long product1, Long product2) {
        matrix.computeIfAbsent(product1, k -> new HashMap<>())
              .merge(product2, 1, Integer::sum);
    }

    /**
     * Compute similarity scores using Cosine Similarity
     * Similarity(A, B) = CoOccurrence(A, B) / sqrt(TotalViews(A) * TotalViews(B))
     */
    private Map<Long, Map<Long, Double>> computeSimilarityMatrix(
            Map<Long, Map<Long, Integer>> coOccurrenceMatrix) {

        Map<Long, Map<Long, Double>> similarityMatrix = new HashMap<>();

        // Calculate total co-occurrences for each product (for normalization)
        Map<Long, Integer> totalCoOccurrences = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : coOccurrenceMatrix.entrySet()) {
            Long productId = entry.getKey();
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            totalCoOccurrences.put(productId, total);
        }

        // Compute similarity for each product pair
        for (Map.Entry<Long, Map<Long, Integer>> entry : coOccurrenceMatrix.entrySet()) {
            Long product1 = entry.getKey();
            Map<Long, Integer> coOccurrences = entry.getValue();

            Map<Long, Double> similarities = new HashMap<>();

            for (Map.Entry<Long, Integer> coEntry : coOccurrences.entrySet()) {
                Long product2 = coEntry.getKey();
                Integer coOccurrence = coEntry.getValue();

                // Cosine similarity
                int total1 = totalCoOccurrences.getOrDefault(product1, 1);
                int total2 = totalCoOccurrences.getOrDefault(product2, 1);

                double similarity = coOccurrence / Math.sqrt(total1 * total2);
                similarities.put(product2, similarity);
            }

            similarityMatrix.put(product1, similarities);
        }

        return similarityMatrix;
    }

    /**
     * Store top N similar products in Redis Sorted Sets
     */
    private void storeRecommendations(Map<Long, Map<Long, Double>> similarityMatrix) {
        int storedCount = 0;

        for (Map.Entry<Long, Map<Long, Double>> entry : similarityMatrix.entrySet()) {
            Long productId = entry.getKey();
            Map<Long, Double> similarities = entry.getValue();

            // Sort by similarity score and take top N
            List<Map.Entry<Long, Double>> topSimilar = similarities.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(TOP_N_SIMILAR)
                .collect(Collectors.toList());

            // Store in Redis Sorted Set
            String key = RECOMMEND_KEY_PREFIX + productId;

            // Clear old recommendations
            redisTemplate.delete(key);

            // Add new recommendations
            for (Map.Entry<Long, Double> similar : topSimilar) {
                redisTemplate.opsForZSet().add(
                    key,
                    similar.getKey().toString(),
                    similar.getValue()
                );
            }

            storedCount++;
        }

        System.out.println("Stored recommendations for " + storedCount + " products");
    }

    /**
     * Manually trigger recommendation computation (for testing/admin)
     */
    public void triggerComputation() {
        System.out.println("Manual recommendation computation triggered");
        computeRecommendations();
    }

    /**
     * Get similar products for a given product (from pre-computed cache)
     */
    public List<Long> getSimilarProducts(Long productId, int limit) {
        String key = RECOMMEND_KEY_PREFIX + productId;

        // Get top N similar products (highest scores first)
        Set<Object> similarIds = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);

        if (similarIds == null || similarIds.isEmpty()) {
            return Collections.emptyList();
        }

        return similarIds.stream()
            .map(obj -> Long.parseLong(obj.toString()))
            .collect(Collectors.toList());
    }
}

