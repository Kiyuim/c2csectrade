package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ReviewRequest;
import lut.cn.c2cplatform.dto.ReviewResponse;
import lut.cn.c2cplatform.entity.Review;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 创建评价
     */
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewRequest request, Authentication authentication) {
        try {
            // 从用户名获取用户信息
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }

            Integer userId = user.getId().intValue();
            Review review = reviewService.createReview(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "评价成功");
            response.put("data", review);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 检查订单是否已评价
     */
    @GetMapping("/check/{orderId}")
    public ResponseEntity<?> checkReview(@PathVariable Integer orderId) {
        try {
            boolean hasReviewed = reviewService.hasReviewed(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasReviewed", hasReviewed);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取商品的所有评价
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductReviews(@PathVariable Long productId) {
        try {
            List<ReviewResponse> reviews = reviewService.getProductReviews(productId);
            Double averageRating = reviewService.getProductAverageRating(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            response.put("averageRating", averageRating);
            response.put("totalReviews", reviews.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户作为卖家的评价
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerReviews(@PathVariable Integer sellerId) {
        try {
            List<ReviewResponse> reviews = reviewService.getSellerReviews(sellerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户作为买家的评价
     */
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<?> getBuyerReviews(@PathVariable Integer buyerId) {
        try {
            List<ReviewResponse> reviews = reviewService.getBuyerReviews(buyerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

