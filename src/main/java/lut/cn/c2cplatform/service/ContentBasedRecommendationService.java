package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Content-Based Recommendation Service
 * Uses TF-IDF and Cosine Similarity for product similarity computation
 * Considers: name, description, category, price range, condition
 */
@Service
public class ContentBasedRecommendationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    private static final String CONTENT_SIMILARITY_PREFIX = "recommend:content:";
    private static final int TOP_N_SIMILAR = 15;

    /**
     * Compute content-based similarity for all products
     * Uses TF-IDF on product text features
     */
    public void computeContentBasedSimilarity() {
        System.out.println("=== Starting content-based similarity computation ===");
        long startTime = System.currentTimeMillis();

        try {
            // Get all active products
            List<Product> allProducts = productMapper.selectAllActive();
            if (allProducts.isEmpty()) {
                System.out.println("No products available for content-based recommendation");
                return;
            }

            System.out.println("Processing " + allProducts.size() + " products");

            // Build document corpus and compute IDF
            Map<String, Integer> documentFrequency = buildDocumentFrequency(allProducts);
            int totalDocuments = allProducts.size();

            // Compute TF-IDF vectors for each product
            Map<Long, Map<String, Double>> tfidfVectors = new HashMap<>();
            for (Product product : allProducts) {
                Map<String, Double> tfidf = computeTFIDF(product, documentFrequency, totalDocuments);
                tfidfVectors.put(product.getId(), tfidf);
            }

            // Compute pairwise similarity and store top N for each product
            int processedCount = 0;
            for (Product product : allProducts) {
                Map<Long, Double> similarities = new HashMap<>();
                Map<String, Double> vector1 = tfidfVectors.get(product.getId());

                for (Product other : allProducts) {
                    if (product.getId().equals(other.getId())) continue;

                    Map<String, Double> vector2 = tfidfVectors.get(other.getId());
                    double similarity = cosineSimilarity(vector1, vector2);

                    // Add price similarity factor (products with similar prices are more similar)
                    double priceSimilarity = computePriceSimilarity(
                        product.getPrice() != null ? product.getPrice().doubleValue() : 0.0,
                        other.getPrice() != null ? other.getPrice().doubleValue() : 0.0
                    );
                    similarity = similarity * 0.8 + priceSimilarity * 0.2;

                    // Add condition similarity
                    if (product.getConditionLevel() != null && other.getConditionLevel() != null) {
                        int conditionDiff = Math.abs(product.getConditionLevel() - other.getConditionLevel());
                        double conditionSimilarity = 1.0 - (conditionDiff / 10.0);
                        similarity = similarity * 0.9 + conditionSimilarity * 0.1;
                    }

                    similarities.put(other.getId(), similarity);
                }

                // Store top N similar products
                storeTopSimilar(product.getId(), similarities);
                processedCount++;

                if (processedCount % 100 == 0) {
                    System.out.println("Processed " + processedCount + " / " + allProducts.size() + " products");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("=== Content-based similarity computation completed in " + duration + "ms ===");

        } catch (Exception e) {
            System.err.println("Failed to compute content-based similarity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build document frequency map for IDF calculation
     */
    private Map<String, Integer> buildDocumentFrequency(List<Product> products) {
        Map<String, Integer> df = new HashMap<>();

        for (Product product : products) {
            Set<String> terms = extractTerms(product);
            for (String term : terms) {
                df.merge(term, 1, Integer::sum);
            }
        }

        return df;
    }

    /**
     * Extract terms from product for text analysis
     */
    private Set<String> extractTerms(Product product) {
        Set<String> terms = new HashSet<>();

        // Category (重要性高，添加多次)
        if (product.getCategory() != null && !product.getCategory().trim().isEmpty()) {
            String category = product.getCategory().toLowerCase();
            terms.add("cat:" + category);
            terms.add("cat:" + category); // Add twice for higher weight
        }

        // Product name (分词并提取关键词)
        if (product.getName() != null) {
            String[] nameWords = tokenize(product.getName());
            for (String word : nameWords) {
                if (word.length() > 1) { // Filter out single characters
                    terms.add("name:" + word);
                }
            }
        }

        // Description (较低权重)
        if (product.getDescription() != null) {
            String[] descWords = tokenize(product.getDescription());
            for (String word : descWords) {
                if (word.length() > 2) { // Longer words more meaningful
                    terms.add("desc:" + word);
                }
            }
        }

        // Price range bucket
        String priceRange = getPriceRangeBucket(product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
        terms.add("price:" + priceRange);

        // Condition level
        if (product.getConditionLevel() != null) {
            terms.add("condition:" + product.getConditionLevel());
        }

        // Location (区域相似性)
        if (product.getLocation() != null && !product.getLocation().trim().isEmpty()) {
            terms.add("loc:" + product.getLocation().toLowerCase());
        }

        return terms;
    }

    /**
     * Simple Chinese/English tokenizer
     */
    private String[] tokenize(String text) {
        if (text == null) return new String[0];

        text = text.toLowerCase();
        // Split by non-word characters, keep Chinese characters
        String[] tokens = text.split("[^\\p{L}\\p{N}]+");

        // For Chinese, also split into bigrams for better matching
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                result.add(token);

                // Add bigrams for Chinese text
                if (containsChinese(token) && token.length() >= 2) {
                    for (int i = 0; i < token.length() - 1; i++) {
                        result.add(token.substring(i, i + 2));
                    }
                }
            }
        }

        return result.toArray(new String[0]);
    }

    /**
     * Check if string contains Chinese characters
     */
    private boolean containsChinese(String str) {
        return str.matches(".*[\\u4e00-\\u9fa5]+.*");
    }

    /**
     * Get price range bucket for grouping
     */
    private String getPriceRangeBucket(Double price) {
        if (price == null || price <= 0) return "unknown";
        if (price < 20) return "0-20";
        if (price < 50) return "20-50";
        if (price < 100) return "50-100";
        if (price < 200) return "100-200";
        if (price < 500) return "200-500";
        return "500+";
    }

    /**
     * Compute TF-IDF vector for a product
     */
    private Map<String, Double> computeTFIDF(Product product, Map<String, Integer> df, int totalDocs) {
        Map<String, Double> tfidf = new HashMap<>();
        List<String> terms = new ArrayList<>(extractTerms(product));

        // Calculate term frequency
        Map<String, Long> tf = terms.stream()
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        // Calculate TF-IDF
        for (Map.Entry<String, Long> entry : tf.entrySet()) {
            String term = entry.getKey();
            double termFreq = entry.getValue();
            int docFreq = df.getOrDefault(term, 1);

            // TF-IDF = (term freq / total terms) * log(total docs / doc freq)
            double tfidfScore = (termFreq / terms.size()) * Math.log((double) totalDocs / docFreq);
            tfidf.put(term, tfidfScore);
        }

        return tfidf;
    }

    /**
     * Compute cosine similarity between two TF-IDF vectors
     */
    private double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // Compute dot product and norms
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(vec1.keySet());
        allTerms.addAll(vec2.keySet());

        for (String term : allTerms) {
            double val1 = vec1.getOrDefault(term, 0.0);
            double val2 = vec2.getOrDefault(term, 0.0);

            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Compute price similarity (exponential decay based on price difference)
     */
    private double computePriceSimilarity(Double price1, Double price2) {
        if (price1 == null || price2 == null || price1 <= 0 || price2 <= 0) {
            return 0.5; // Neutral similarity
        }

        double avgPrice = (price1 + price2) / 2;
        double priceDiff = Math.abs(price1 - price2);
        double relativeDiff = priceDiff / avgPrice;

        // Exponential decay: similarity decreases as price difference increases
        return Math.exp(-relativeDiff * 2);
    }

    /**
     * Store top N similar products in Redis
     */
    private void storeTopSimilar(Long productId, Map<Long, Double> similarities) {
        String key = CONTENT_SIMILARITY_PREFIX + productId;

        // Sort and get top N
        List<Map.Entry<Long, Double>> topSimilar = similarities.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(TOP_N_SIMILAR)
            .collect(Collectors.toList());

        // Clear old data
        redisTemplate.delete(key);

        // Store in Redis Sorted Set
        for (Map.Entry<Long, Double> entry : topSimilar) {
            redisTemplate.opsForZSet().add(key, entry.getKey().toString(), entry.getValue());
        }

        // Set expiration (7 days)
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    /**
     * Get content-based similar products
     */
    public List<Long> getContentBasedSimilar(Long productId, int limit) {
        String key = CONTENT_SIMILARITY_PREFIX + productId;

        Set<Object> similarIds = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);

        if (similarIds == null || similarIds.isEmpty()) {
            return Collections.emptyList();
        }

        return similarIds.stream()
            .map(obj -> Long.parseLong(obj.toString()))
            .collect(Collectors.toList());
    }
}

