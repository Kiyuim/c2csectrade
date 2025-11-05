package lut.cn.c2cplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A/B Testing Framework for Recommendation Strategies
 * Allows testing different recommendation algorithms and strategies
 */
@Service
public class ABTestService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String AB_TEST_CONFIG_PREFIX = "abtest:config:";
    private static final String AB_TEST_USER_GROUP_PREFIX = "abtest:user:";
    private static final String AB_TEST_METRICS_PREFIX = "abtest:metrics:";

    /**
     * A/B Test Experiment Configuration
     */
    public static class Experiment {
        private String experimentId;
        private String name;
        private Map<String, Double> variants; // variant_name -> traffic_percentage
        private Date startDate;
        private Date endDate;
        private boolean active;

        public Experiment(String experimentId, String name) {
            this.experimentId = experimentId;
            this.name = name;
            this.variants = new HashMap<>();
            this.active = true;
        }

        // Getters and setters
        public String getExperimentId() { return experimentId; }
        public String getName() { return name; }
        public Map<String, Double> getVariants() { return variants; }
        public boolean isActive() { return active; }

        public void addVariant(String variantName, double percentage) {
            variants.put(variantName, percentage);
        }
    }

    /**
     * Create new A/B test experiment
     */
    public void createExperiment(String experimentId, String name, Map<String, Double> variants) {
        Experiment experiment = new Experiment(experimentId, name);
        variants.forEach(experiment::addVariant);

        String key = AB_TEST_CONFIG_PREFIX + experimentId;
        redisTemplate.opsForValue().set(key, experiment, 30, TimeUnit.DAYS);

        System.out.println("Created A/B test experiment: " + name);
    }

    /**
     * Assign user to a variant (consistent hashing)
     */
    public String assignUserToVariant(Long userId, String experimentId) {
        // Check if user already assigned
        String userKey = AB_TEST_USER_GROUP_PREFIX + experimentId + ":" + userId;
        Object existingVariant = redisTemplate.opsForValue().get(userKey);

        if (existingVariant != null) {
            return existingVariant.toString();
        }

        // Get experiment config
        String configKey = AB_TEST_CONFIG_PREFIX + experimentId;
        Object expObj = redisTemplate.opsForValue().get(configKey);

        if (!(expObj instanceof Experiment)) {
            return "control"; // Default to control group
        }

        Experiment experiment = (Experiment) expObj;

        // Assign based on user ID hash (consistent assignment)
        String variant = assignVariant(userId, experiment.getVariants());

        // Store assignment
        redisTemplate.opsForValue().set(userKey, variant, 30, TimeUnit.DAYS);

        return variant;
    }

    /**
     * Get recommendation strategy based on A/B test variant
     */
    public String getRecommendationStrategy(Long userId) {
        String experimentId = "recommendation_algorithm_v1";
        String variant = assignUserToVariant(userId, experimentId);

        return switch (variant) {
            case "control" -> "collaborative_filtering";
            case "variant_a" -> "content_based";
            case "variant_b" -> "hybrid";
            case "variant_c" -> "ncf_neural";
            case "variant_d" -> "realtime";
            default -> "collaborative_filtering";
        };
    }

    /**
     * Track A/B test metrics
     */
    public void trackMetric(String experimentId, String variant, String metricName, double value) {
        String key = AB_TEST_METRICS_PREFIX + experimentId + ":" + variant + ":" + metricName;

        // Increment counter
        redisTemplate.opsForValue().increment(key);

        // Store value in list for statistical analysis
        String valuesKey = key + ":values";
        redisTemplate.opsForList().rightPush(valuesKey, value);

        // Keep only last 10000 values
        Long size = redisTemplate.opsForList().size(valuesKey);
        if (size != null && size > 10000) {
            redisTemplate.opsForList().trim(valuesKey, -10000, -1);
        }
    }

    /**
     * Track recommendation click (conversion)
     */
    public void trackClick(Long userId, String experimentId, Long productId) {
        String variant = assignUserToVariant(userId, experimentId);

        // Track click rate
        trackMetric(experimentId, variant, "clicks", 1.0);

        // Track unique user clicks
        String uniqueKey = AB_TEST_METRICS_PREFIX + experimentId + ":" + variant + ":unique_users";
        redisTemplate.opsForSet().add(uniqueKey, userId.toString());
    }

    /**
     * Track recommendation conversion (order)
     */
    public void trackConversion(Long userId, String experimentId, Long productId, double orderAmount) {
        String variant = assignUserToVariant(userId, experimentId);

        // Track conversion rate
        trackMetric(experimentId, variant, "conversions", 1.0);

        // Track revenue
        trackMetric(experimentId, variant, "revenue", orderAmount);
    }

    /**
     * Get A/B test results
     */
    public Map<String, Map<String, Object>> getExperimentResults(String experimentId) {
        String configKey = AB_TEST_CONFIG_PREFIX + experimentId;
        Object expObj = redisTemplate.opsForValue().get(configKey);

        if (!(expObj instanceof Experiment)) {
            return Collections.emptyMap();
        }

        Experiment experiment = (Experiment) expObj;
        Map<String, Map<String, Object>> results = new HashMap<>();

        for (String variant : experiment.getVariants().keySet()) {
            Map<String, Object> variantResults = new HashMap<>();

            // Get impression count
            String impressionKey = AB_TEST_METRICS_PREFIX + experimentId + ":" + variant + ":impressions";
            Object impressions = redisTemplate.opsForValue().get(impressionKey);
            long impressionCount = impressions instanceof Long ? (Long) impressions : 0;

            // Get click count
            String clickKey = AB_TEST_METRICS_PREFIX + experimentId + ":" + variant + ":clicks";
            Object clicks = redisTemplate.opsForValue().get(clickKey);
            long clickCount = clicks instanceof Long ? (Long) clicks : 0;

            // Get conversion count
            String conversionKey = AB_TEST_METRICS_PREFIX + experimentId + ":" + variant + ":conversions";
            Object conversions = redisTemplate.opsForValue().get(conversionKey);
            long conversionCount = conversions instanceof Long ? (Long) conversions : 0;

            // Get revenue
            String revenueKey = AB_TEST_METRICS_PREFIX + experimentId + ":" + variant + ":revenue:values";
            List<Object> revenueValues = redisTemplate.opsForList().range(revenueKey, 0, -1);
            double totalRevenue = revenueValues != null ?
                revenueValues.stream()
                    .filter(obj -> obj instanceof Double)
                    .mapToDouble(obj -> (Double) obj)
                    .sum() : 0.0;

            // Calculate metrics
            double ctr = impressionCount > 0 ? (double) clickCount / impressionCount : 0.0;
            double cvr = clickCount > 0 ? (double) conversionCount / clickCount : 0.0;
            double avgRevenue = conversionCount > 0 ? totalRevenue / conversionCount : 0.0;

            variantResults.put("impressions", impressionCount);
            variantResults.put("clicks", clickCount);
            variantResults.put("conversions", conversionCount);
            variantResults.put("ctr", ctr);
            variantResults.put("cvr", cvr);
            variantResults.put("totalRevenue", totalRevenue);
            variantResults.put("avgRevenue", avgRevenue);

            results.put(variant, variantResults);
        }

        return results;
    }

    /**
     * Statistical significance test (simplified Chi-square test)
     */
    public boolean isStatisticallySignificant(String experimentId, String variantA, String variantB) {
        Map<String, Map<String, Object>> results = getExperimentResults(experimentId);

        Map<String, Object> dataA = results.get(variantA);
        Map<String, Object> dataB = results.get(variantB);

        if (dataA == null || dataB == null) {
            return false;
        }

        long clicksA = (long) dataA.getOrDefault("clicks", 0L);
        long clicksB = (long) dataB.getOrDefault("clicks", 0L);
        long impressionsA = (long) dataA.getOrDefault("impressions", 0L);
        long impressionsB = (long) dataB.getOrDefault("impressions", 0L);

        // Need sufficient sample size
        if (impressionsA < 100 || impressionsB < 100) {
            return false;
        }

        // Chi-square test
        double pA = (double) clicksA / impressionsA;
        double pB = (double) clicksB / impressionsB;
        double pooledP = (double) (clicksA + clicksB) / (impressionsA + impressionsB);

        double se = Math.sqrt(pooledP * (1 - pooledP) * (1.0/impressionsA + 1.0/impressionsB));
        double zScore = Math.abs(pA - pB) / se;

        // Z-score > 1.96 indicates 95% confidence
        return zScore > 1.96;
    }

    /**
     * Assign variant based on consistent hashing
     */
    private String assignVariant(Long userId, Map<String, Double> variants) {
        double hash = (userId.hashCode() & 0xfffffff) / (double) 0xfffffff;

        double cumulative = 0.0;
        for (Map.Entry<String, Double> entry : variants.entrySet()) {
            cumulative += entry.getValue();
            if (hash < cumulative) {
                return entry.getKey();
            }
        }

        return variants.keySet().iterator().next(); // Fallback
    }

    /**
     * Initialize default recommendation A/B test
     */
    public void initializeDefaultExperiment() {
        Map<String, Double> variants = new HashMap<>();
        variants.put("control", 0.2);        // 20% - CF only
        variants.put("variant_a", 0.2);      // 20% - Content-based
        variants.put("variant_b", 0.2);      // 20% - Hybrid
        variants.put("variant_c", 0.2);      // 20% - NCF
        variants.put("variant_d", 0.2);      // 20% - Real-time

        createExperiment("recommendation_algorithm_v1", "Recommendation Algorithm Comparison", variants);
    }
}

