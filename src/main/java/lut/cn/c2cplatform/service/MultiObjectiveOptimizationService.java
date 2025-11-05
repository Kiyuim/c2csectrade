package lut.cn.c2cplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-Objective Optimization Service
 * Optimizes recommendations for multiple goals simultaneously:
 * 1. Click-Through Rate (CTR)
 * 2. Conversion Rate (CVR)
 * 3. Revenue
 * 4. Diversity
 * 5. User Satisfaction
 *
 * Uses weighted scoring and Pareto optimization
 */
@Service
public class MultiObjectiveOptimizationService {

    @Autowired
    private NCFRecommendationService ncfService;

    @Autowired
    private HybridRecommendationService hybridService;

    @Autowired
    private RealtimeRecommendationService realtimeService;

    // Objective weights (configurable via A/B testing)
    private double ctrWeight = 0.25;
    private double cvrWeight = 0.30;
    private double revenueWeight = 0.20;
    private double diversityWeight = 0.15;
    private double noveltyWeight = 0.10;

    /**
     * Recommendation candidate with multi-objective scores
     */
    public static class ScoredCandidate {
        private Long productId;
        private double ctrScore;
        private double cvrScore;
        private double revenueScore;
        private double diversityScore;
        private double noveltyScore;
        private double compositeScore;

        public ScoredCandidate(Long productId) {
            this.productId = productId;
        }

        public Long getProductId() { return productId; }
        public double getCompositeScore() { return compositeScore; }

        public void computeCompositeScore(double ctrW, double cvrW, double revW, double divW, double novW) {
            this.compositeScore = ctrW * ctrScore + cvrW * cvrScore +
                                 revW * revenueScore + divW * diversityScore + novW * noveltyScore;
        }

        public void setCtrScore(double score) { this.ctrScore = score; }
        public void setCvrScore(double score) { this.cvrScore = score; }
        public void setRevenueScore(double score) { this.revenueScore = score; }
        public void setDiversityScore(double score) { this.diversityScore = score; }
        public void setNoveltyScore(double score) { this.noveltyScore = score; }
    }

    /**
     * Get optimized recommendations considering multiple objectives
     */
    public List<Long> getMultiObjectiveRecommendations(Long userId, List<Long> candidates, int topK) {
        List<ScoredCandidate> scoredCandidates = new ArrayList<>();

        for (Long productId : candidates) {
            ScoredCandidate candidate = new ScoredCandidate(productId);

            // 1. Predict CTR (click-through rate)
            candidate.setCtrScore(predictCTR(userId, productId));

            // 2. Predict CVR (conversion rate)
            candidate.setCvrScore(predictCVR(userId, productId));

            // 3. Predict Revenue
            candidate.setRevenueScore(predictRevenue(userId, productId));

            scoredCandidates.add(candidate);
        }

        // 4. Calculate diversity scores
        calculateDiversityScores(scoredCandidates);

        // 5. Calculate novelty scores
        calculateNoveltyScores(userId, scoredCandidates);

        // 6. Compute composite scores
        for (ScoredCandidate candidate : scoredCandidates) {
            candidate.computeCompositeScore(ctrWeight, cvrWeight, revenueWeight,
                                           diversityWeight, noveltyWeight);
        }

        // 7. Sort and select top-K
        return scoredCandidates.stream()
            .sorted((c1, c2) -> Double.compare(c2.getCompositeScore(), c1.getCompositeScore()))
            .limit(topK)
            .map(ScoredCandidate::getProductId)
            .collect(Collectors.toList());
    }

    /**
     * Re-rank recommendations for diversity
     * Uses Maximal Marginal Relevance (MMR)
     */
    public List<Long> rerankForDiversity(List<Long> recommendations, double lambda) {
        if (recommendations.size() <= 1) {
            return recommendations;
        }

        List<Long> reranked = new ArrayList<>();
        Set<Long> remaining = new HashSet<>(recommendations);

        // Start with highest scored item
        Long first = recommendations.get(0);
        reranked.add(first);
        remaining.remove(first);

        // Iteratively select items balancing relevance and diversity
        while (!remaining.isEmpty() && reranked.size() < recommendations.size()) {
            Long bestNext = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Long candidate : remaining) {
                // Relevance score (original ranking position)
                double relevance = 1.0 / (recommendations.indexOf(candidate) + 1);

                // Diversity score (dissimilarity to already selected items)
                double diversity = 0.0;
                for (Long selected : reranked) {
                    diversity += calculateDissimilarity(candidate, selected);
                }
                diversity /= reranked.size();

                // MMR score
                double mmrScore = lambda * relevance + (1 - lambda) * diversity;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestNext = candidate;
                }
            }

            if (bestNext != null) {
                reranked.add(bestNext);
                remaining.remove(bestNext);
            } else {
                break;
            }
        }

        return reranked;
    }

    /**
     * Predict Click-Through Rate
     */
    private double predictCTR(Long userId, Long productId) {
        // Use NCF for CTR prediction
        double ncfScore = ncfService.predictScore(userId, productId);

        // Boost with real-time trending signal
        double trendingBoost = realtimeService.getTrendingScore(productId);

        return 0.7 * ncfScore + 0.3 * trendingBoost;
    }

    /**
     * Predict Conversion Rate
     */
    private double predictCVR(Long userId, Long productId) {
        // CVR is typically lower than CTR
        // Use user's historical conversion pattern
        double baseCVR = 0.1; // Average CVR

        // Adjust based on user affinity
        double userAffinity = ncfService.predictScore(userId, productId);

        return baseCVR * userAffinity;
    }

    /**
     * Predict Revenue (expected value)
     */
    private double predictRevenue(Long userId, Long productId) {
        // Revenue = CVR * Average Order Value
        double cvr = predictCVR(userId, productId);

        // Estimate product value (simplified - would use actual price in production)
        double estimatedValue = 50.0; // Placeholder

        return cvr * estimatedValue;
    }

    /**
     * Calculate diversity scores (intra-list diversity)
     */
    private void calculateDiversityScores(List<ScoredCandidate> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            ScoredCandidate candidate = candidates.get(i);
            double diversitySum = 0.0;

            for (int j = 0; j < candidates.size(); j++) {
                if (i != j) {
                    diversitySum += calculateDissimilarity(
                        candidate.getProductId(),
                        candidates.get(j).getProductId()
                    );
                }
            }

            double avgDiversity = candidates.size() > 1 ?
                diversitySum / (candidates.size() - 1) : 0.0;

            candidate.setDiversityScore(avgDiversity);
        }
    }

    /**
     * Calculate novelty scores (new vs explored)
     */
    private void calculateNoveltyScores(Long userId, List<ScoredCandidate> candidates) {
        // Check which products user has seen before
        // Higher score for unseen products
        for (ScoredCandidate candidate : candidates) {
            // Simplified: assume 70% novelty for now
            // In production, check against user history
            candidate.setNoveltyScore(0.7);
        }
    }

    /**
     * Calculate dissimilarity between two products
     * Returns value between 0 (identical) and 1 (completely different)
     */
    private double calculateDissimilarity(Long productId1, Long productId2) {
        if (productId1.equals(productId2)) {
            return 0.0;
        }

        // Use content-based features for dissimilarity
        // Simplified: use product ID difference normalized
        double diff = Math.abs(productId1 - productId2);
        return Math.min(1.0, diff / 1000.0);
    }

    /**
     * Pareto optimization: find non-dominated solutions
     */
    public List<ScoredCandidate> getParetoFrontier(List<ScoredCandidate> candidates) {
        List<ScoredCandidate> paretoFrontier = new ArrayList<>();

        for (ScoredCandidate candidate : candidates) {
            boolean isDominated = false;

            for (ScoredCandidate other : candidates) {
                if (dominates(other, candidate)) {
                    isDominated = true;
                    break;
                }
            }

            if (!isDominated) {
                paretoFrontier.add(candidate);
            }
        }

        return paretoFrontier;
    }

    /**
     * Check if candidate1 dominates candidate2 (Pareto dominance)
     */
    private boolean dominates(ScoredCandidate c1, ScoredCandidate c2) {
        return c1.ctrScore >= c2.ctrScore &&
               c1.cvrScore >= c2.cvrScore &&
               c1.revenueScore >= c2.revenueScore &&
               c1.diversityScore >= c2.diversityScore &&
               c1.noveltyScore >= c2.noveltyScore &&
               (c1.ctrScore > c2.ctrScore ||
                c1.cvrScore > c2.cvrScore ||
                c1.revenueScore > c2.revenueScore ||
                c1.diversityScore > c2.diversityScore ||
                c1.noveltyScore > c2.noveltyScore);
    }

    /**
     * Update objective weights (for online learning)
     */
    public void updateWeights(double ctr, double cvr, double revenue, double diversity, double novelty) {
        // Normalize weights to sum to 1.0
        double sum = ctr + cvr + revenue + diversity + novelty;
        this.ctrWeight = ctr / sum;
        this.cvrWeight = cvr / sum;
        this.revenueWeight = revenue / sum;
        this.diversityWeight = diversity / sum;
        this.noveltyWeight = novelty / sum;
    }
}

