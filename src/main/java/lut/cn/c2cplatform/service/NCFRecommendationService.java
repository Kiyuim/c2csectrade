package lut.cn.c2cplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Neural Collaborative Filtering (NCF) Service
 * Implements a simplified neural network approach for recommendations
 *
 * Architecture:
 * - Embedding Layer: User and Item embeddings
 * - MLP Layers: Multi-layer perceptron for feature learning
 * - Prediction Layer: Final score prediction
 *
 * Note: This is a simplified version. For production, consider using:
 * - TensorFlow/PyTorch for actual deep learning
 * - Separate Python service for model training
 * - REST API for model inference
 */
@Service
public class NCFRecommendationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Lazy
    @Autowired
    private HistoryService historyService;

    private static final String NCF_EMBEDDING_PREFIX = "ncf:embedding:";
    private static final String NCF_PREDICTION_PREFIX = "ncf:prediction:";
    private static final int EMBEDDING_DIM = 32;
    private static final double LEARNING_RATE = 0.001;

    /**
     * Initialize user and item embeddings
     * In production, these would be learned from a neural network
     */
    public void initializeEmbeddings(Long userId) {
        String key = NCF_EMBEDDING_PREFIX + "user:" + userId;

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            // Initialize with random values (in production, load from trained model)
            double[] embedding = new double[EMBEDDING_DIM];
            Random random = new Random(userId);
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] = random.nextGaussian() * 0.1; // Xavier initialization
            }

            redisTemplate.opsForValue().set(key, embedding, 7, TimeUnit.DAYS);
        }
    }

    public void initializeItemEmbedding(Long productId) {
        String key = NCF_EMBEDDING_PREFIX + "item:" + productId;

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            double[] embedding = new double[EMBEDDING_DIM];
            Random random = new Random(productId);
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] = random.nextGaussian() * 0.1;
            }

            redisTemplate.opsForValue().set(key, embedding, 7, TimeUnit.DAYS);
        }
    }

    /**
     * Predict user-item interaction score using NCF
     * Combines GMF (Generalized Matrix Factorization) and MLP
     */
    public double predictScore(Long userId, Long productId) {
        initializeEmbeddings(userId);
        initializeItemEmbedding(productId);

        // Get embeddings
        double[] userEmbedding = getUserEmbedding(userId);
        double[] itemEmbedding = getItemEmbedding(productId);

        if (userEmbedding == null || itemEmbedding == null) {
            return 0.0;
        }

        // GMF part: Element-wise product
        double gmfScore = computeGMF(userEmbedding, itemEmbedding);

        // MLP part: Concatenate and pass through neural network
        double mlpScore = computeMLP(userEmbedding, itemEmbedding);

        // Combine scores (NeuMF: Neural Matrix Factorization)
        double finalScore = 0.5 * gmfScore + 0.5 * mlpScore;

        // Apply sigmoid for normalization
        return sigmoid(finalScore);
    }

    /**
     * Get top-K recommendations for user using NCF
     */
    public List<Long> getNCFRecommendations(Long userId, List<Long> candidateProducts, int topK) {
        Map<Long, Double> scores = new HashMap<>();

        for (Long productId : candidateProducts) {
            double score = predictScore(userId, productId);
            scores.put(productId, score);
        }

        return scores.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Update embeddings based on user interaction (online learning)
     * This is a simplified version of backpropagation
     */
    public void updateEmbeddings(Long userId, Long productId, double actualInteraction) {
        double predictedScore = predictScore(userId, productId);
        double error = actualInteraction - predictedScore;

        // Gradient descent update (simplified)
        double[] userEmbedding = getUserEmbedding(userId);
        double[] itemEmbedding = getItemEmbedding(productId);

        if (userEmbedding != null && itemEmbedding != null) {
            // Update user embedding
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                userEmbedding[i] += LEARNING_RATE * error * itemEmbedding[i];
            }

            // Update item embedding
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                itemEmbedding[i] += LEARNING_RATE * error * userEmbedding[i];
            }

            // Save updated embeddings
            saveUserEmbedding(userId, userEmbedding);
            saveItemEmbedding(productId, itemEmbedding);
        }
    }

    /**
     * Train on user interaction (view=0.3, cart=0.5, order=1.0)
     */
    public void trainOnInteraction(Long userId, Long productId, String interactionType) {
        double label = switch (interactionType.toLowerCase()) {
            case "view" -> 0.3;
            case "favorite" -> 0.5;
            case "cart" -> 0.7;
            case "order" -> 1.0;
            default -> 0.1;
        };

        updateEmbeddings(userId, productId, label);
    }

    /**
     * Generalized Matrix Factorization
     */
    private double computeGMF(double[] userEmb, double[] itemEmb) {
        double sum = 0.0;
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            sum += userEmb[i] * itemEmb[i];
        }
        return sum;
    }

    /**
     * Multi-Layer Perceptron component
     * Simplified 2-layer MLP
     */
    private double computeMLP(double[] userEmb, double[] itemEmb) {
        // Concatenate embeddings
        double[] concat = new double[EMBEDDING_DIM * 2];
        System.arraycopy(userEmb, 0, concat, 0, EMBEDDING_DIM);
        System.arraycopy(itemEmb, 0, concat, EMBEDDING_DIM, EMBEDDING_DIM);

        // Layer 1: Hidden layer with ReLU
        int hiddenSize = 64;
        double[] hidden = new double[hiddenSize];
        Random random = new Random(42);

        for (int i = 0; i < hiddenSize; i++) {
            double sum = 0.0;
            for (int j = 0; j < concat.length; j++) {
                // Simplified weight (in production, load from trained model)
                double weight = random.nextGaussian() * 0.1;
                sum += concat[j] * weight;
            }
            hidden[i] = relu(sum);
        }

        // Layer 2: Output layer
        double output = 0.0;
        for (int i = 0; i < hiddenSize; i++) {
            double weight = random.nextGaussian() * 0.1;
            output += hidden[i] * weight;
        }

        return output;
    }

    private double relu(double x) {
        return Math.max(0, x);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double[] getUserEmbedding(Long userId) {
        String key = NCF_EMBEDDING_PREFIX + "user:" + userId;
        Object obj = redisTemplate.opsForValue().get(key);
        return obj instanceof double[] ? (double[]) obj : null;
    }

    private double[] getItemEmbedding(Long productId) {
        String key = NCF_EMBEDDING_PREFIX + "item:" + productId;
        Object obj = redisTemplate.opsForValue().get(key);
        return obj instanceof double[] ? (double[]) obj : null;
    }

    private void saveUserEmbedding(Long userId, double[] embedding) {
        String key = NCF_EMBEDDING_PREFIX + "user:" + userId;
        redisTemplate.opsForValue().set(key, embedding, 7, TimeUnit.DAYS);
    }

    private void saveItemEmbedding(Long productId, double[] embedding) {
        String key = NCF_EMBEDDING_PREFIX + "item:" + productId;
        redisTemplate.opsForValue().set(key, embedding, 7, TimeUnit.DAYS);
    }
}

