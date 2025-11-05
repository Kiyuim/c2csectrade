package lut.cn.c2cplatform.service.impl;

import lut.cn.c2cplatform.dto.ReviewRequest;
import lut.cn.c2cplatform.dto.ReviewResponse;
import lut.cn.c2cplatform.entity.*;
import lut.cn.c2cplatform.mapper.*;
import lut.cn.c2cplatform.service.CreditScoreService;
import lut.cn.c2cplatform.service.ReviewService;
import lut.cn.c2cplatform.service.HybridRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CreditScoreService creditScoreService;

    @Autowired(required = false)
    private HybridRecommendationService hybridRecommendationService;

    @Override
    @Transactional
    public Review createReview(Integer userId, ReviewRequest request) {
        // 验证订单
        Order order = orderMapper.findById(request.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 验证订单所属用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权评价此订单");
        }

        // 验证订单状态（只有已完成的订单才能评价）
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("订单未完成，无法评价");
        }

        // 检查是否已评价
        Review existingReview = reviewMapper.selectByOrderId(request.getOrderId());
        if (existingReview != null) {
            throw new RuntimeException("该订单已评价");
        }

        // 验证评分范围
        if (request.getProductRating() < 1 || request.getProductRating() > 5) {
            throw new RuntimeException("商品评分必须在1-5之间");
        }
        if (request.getSellerRating() < 1 || request.getSellerRating() > 5) {
            throw new RuntimeException("卖家评分必须在1-5之间");
        }

        // 获取订单项（假设一个订单只有一个商品，如果有多个商品，可以修改逻辑）
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(request.getOrderId());
        if (orderItems.isEmpty()) {
            throw new RuntimeException("订单项不存在");
        }

        OrderItem orderItem = orderItems.get(0);
        Product product = productMapper.selectById(orderItem.getProductId().longValue());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 创建评价
        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setProductId(product.getId());
        review.setBuyerId(userId);
        review.setSellerId(product.getUserId().intValue());
        review.setProductRating(request.getProductRating());
        review.setSellerRating(request.getSellerRating());
        review.setComment(request.getComment());
        review.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);

        // 处理评价图片
        if (request.getReviewImages() != null && !request.getReviewImages().isEmpty()) {
            review.setReviewImages(String.join(",", request.getReviewImages()));
        }

        Instant now = Instant.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewMapper.insert(review);

        // Update product popularity when reviewed
        if (hybridRecommendationService != null) {
            hybridRecommendationService.updatePopularity(product.getId(), "review");
        }

        // 更新卖家信用分
        creditScoreService.updateCreditScore(product.getUserId());

        return review;
    }

    @Override
    public boolean hasReviewed(Integer orderId) {
        Review review = reviewMapper.selectByOrderId(orderId);
        return review != null;
    }

    @Override
    public List<ReviewResponse> getProductReviews(Long productId) {
        List<Review> reviews = reviewMapper.selectByProductId(productId);
        return reviews.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getSellerReviews(Integer sellerId) {
        List<Review> reviews = reviewMapper.selectBySellerId(sellerId);
        return reviews.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getBuyerReviews(Integer buyerId) {
        List<Review> reviews = reviewMapper.selectByBuyerId(buyerId);
        return reviews.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public Double getProductAverageRating(Long productId) {
        Double avg = reviewMapper.getAverageProductRating(productId);
        return avg != null ? avg : 0.0;
    }

    private ReviewResponse convertToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setProductId(review.getProductId());
        response.setSellerId(review.getSellerId());
        response.setProductRating(review.getProductRating());
        response.setSellerRating(review.getSellerRating());
        response.setComment(review.getComment());
        response.setIsAnonymous(review.getIsAnonymous());
        response.setCreatedAt(review.getCreatedAt());

        // 处理评价图片
        if (review.getReviewImages() != null && !review.getReviewImages().isEmpty()) {
            response.setReviewImages(Arrays.asList(review.getReviewImages().split(",")));
        } else {
            response.setReviewImages(new ArrayList<>());
        }

        // 获取商品信息
        Product product = productMapper.selectById(review.getProductId());
        if (product != null) {
            response.setProductName(product.getName());
        }

        // 获取买家信息
        if (review.getIsAnonymous()) {
            response.setBuyerId(null);
            response.setBuyerName("匿名用户");
            response.setBuyerAvatar("https://ui-avatars.com/api/?name=匿名&background=cccccc&color=fff&size=100");
        } else {
            User buyer = userMapper.selectById(review.getBuyerId().longValue());
            if (buyer != null) {
                response.setBuyerId(review.getBuyerId());
                response.setBuyerName(buyer.getDisplayName());
                response.setBuyerAvatar(buyer.getAvatarUrl());
            }
        }

        return response;
    }
}
