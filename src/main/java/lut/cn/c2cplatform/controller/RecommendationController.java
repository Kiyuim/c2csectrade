package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ProductDTO;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.mapper.ProductMapper;
import lut.cn.c2cplatform.security.UserDetailsImpl;
import lut.cn.c2cplatform.service.HistoryService;
import lut.cn.c2cplatform.service.ProductService;
import lut.cn.c2cplatform.service.RecommendationEngineService;
import lut.cn.c2cplatform.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for product recommendations
 * Provides personalized and similar product recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationEngineService recommendationEngine;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService;

    @Autowired(required = false)
    private SearchService searchService;

    /**
     * Get personalized recommendations for current user (猜你喜欢)
     * Based on user's interest profile (tags)
     */
    @GetMapping("/for-you")
    public ResponseEntity<?> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);

            // If user is not logged in, return popular products
            if (userId == null) {
                List<ProductDTO> popularProducts = getPopularProducts(limit);
                return ResponseEntity.ok(popularProducts);
            }

            // Get user's top interests
            Map<String, Integer> interests = historyService.getUserInterestProfile(userId, 3);

            if (interests.isEmpty()) {
                // No interests yet, return popular products
                List<ProductDTO> popularProducts = getPopularProducts(limit);
                return ResponseEntity.ok(popularProducts);
            }

            // Get products matching user's interests
            List<ProductDTO> recommendations = getProductsByInterests(interests, limit, userId);

            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            System.err.println("Failed to get personalized recommendations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get recommendations"));
        }
    }

    /**
     * Get similar products for a specific product (看了又看/相似商品)
     * Based on collaborative filtering
     */
    @GetMapping("/products/{id}/similar")
    public ResponseEntity<?> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            // Get similar product IDs from recommendation engine
            List<Long> similarIds = recommendationEngine.getSimilarProducts(id, limit);

            if (similarIds.isEmpty()) {
                // No pre-computed recommendations, fallback to category-based
                return getCategoryBasedSimilarProducts(id, limit);
            }

            // Get product details with media
            List<Product> products = productMapper.selectByIds(similarIds);

            // Filter out unavailable products and convert to DTO
            List<ProductDTO> productDTOs = products.stream()
                .filter(p -> p.getStatus() == 1) // Only available products
                .map(p -> {
                    // Load full product with media
                    Product fullProduct = productService.getProductById(p.getId());
                    return fullProduct != null ? productService.convertToDTO(fullProduct) : null;
                })
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());

            // If too few results, supplement with category-based recommendations
            if (productDTOs.size() < limit / 2) {
                List<ProductDTO> categoryBased = getCategoryBasedProducts(id, limit - productDTOs.size());
                productDTOs.addAll(categoryBased);
            }

            return ResponseEntity.ok(productDTOs);

        } catch (Exception e) {
            System.err.println("Failed to get similar products: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get similar products"));
        }
    }

    /**
     * Trigger recommendation computation manually (Admin only)
     */
    @PostMapping("/compute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerComputation() {
        try {
            recommendationEngine.triggerComputation();
            return ResponseEntity.ok(Map.of("message", "Recommendation computation triggered"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get products based on user interests
     */
    private List<ProductDTO> getProductsByInterests(Map<String, Integer> interests, int limit, Long userId) {
        Set<Long> viewedProductIds = historyService.getUserViewedProductIds(userId);
        List<ProductDTO> results = new ArrayList<>();

        // Search for products matching each interest tag
        for (String tag : interests.keySet()) {
            try {
                List<Product> products;

                // Try Elasticsearch first if available
                if (searchService != null) {
                    try {
                        // Use the new category search method
                        var productDocuments = searchService.searchProductsByCategory(tag, limit);
                        products = productDocuments.stream()
                            .map(doc -> {
                                try {
                                    return productMapper.selectById(doc.getProductId());
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                            .filter(p -> p != null)
                            .collect(java.util.stream.Collectors.toList());
                    } catch (Exception e) {
                        System.err.println("Elasticsearch search failed, falling back to database: " + e.getMessage());
                        products = productMapper.selectByCategory(tag);
                    }
                } else {
                    // Fallback to database search
                    products = productMapper.selectByCategory(tag);
                }

                for (Product product : products) {
                    if (product.getStatus() == 1 &&
                        !viewedProductIds.contains(product.getId()) &&
                        !product.getUserId().equals(userId)) {
                        // Load full product details with media
                        Product fullProduct = productService.getProductById(product.getId());
                        if (fullProduct != null) {
                            results.add(productService.convertToDTO(fullProduct));
                            if (results.size() >= limit) {
                                return results;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error searching for tag " + tag + ": " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Get popular products (fallback when no personalization data available)
     */
    private List<ProductDTO> getPopularProducts(int limit) {
        try {
            List<Product> products = productMapper.selectRecentProducts(limit * 2); // Get more to randomize
            // Shuffle and filter
            Collections.shuffle(products);
            return products.stream()
                .filter(p -> p.getStatus() == 1)
                .limit(limit)
                .map(p -> {
                    // Load full product with media
                    Product fullProduct = productService.getProductById(p.getId());
                    return fullProduct != null ? productService.convertToDTO(fullProduct) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to get popular products: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fallback: Get similar products based on category
     */
    private ResponseEntity<?> getCategoryBasedSimilarProducts(Long productId, int limit) {
        List<ProductDTO> categoryBased = getCategoryBasedProducts(productId, limit);
        return ResponseEntity.ok(categoryBased);
    }

    /**
     * Get products from same category
     */
    private List<ProductDTO> getCategoryBasedProducts(Long productId, int limit) {
        try {
            Product product = productMapper.selectById(productId);
            if (product == null || product.getCategory() == null) {
                return Collections.emptyList();
            }

            List<Product> similarProducts = productMapper.selectByCategory(product.getCategory());

            return similarProducts.stream()
                .filter(p -> !p.getId().equals(productId) && p.getStatus() == 1)
                .limit(limit)
                .map(p -> {
                    // Load full product with media
                    Product fullProduct = productService.getProductById(p.getId());
                    return fullProduct != null ? productService.convertToDTO(fullProduct) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Failed to get category-based products: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extract user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        }

        return null;
    }
}