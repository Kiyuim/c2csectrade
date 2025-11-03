package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.dto.ReviewRequest;
import lut.cn.c2cplatform.dto.ReviewResponse;
import lut.cn.c2cplatform.entity.Review;

import java.util.List;

public interface ReviewService {

    /**
     * 创建评价
     */
    Review createReview(Integer userId, ReviewRequest request);

    /**
     * 检查订单是否已评价
     */
    boolean hasReviewed(Integer orderId);

    /**
     * 获取商品的所有评价
     */
    List<ReviewResponse> getProductReviews(Long productId);

    /**
     * 获取用户作为卖家的评价
     */
    List<ReviewResponse> getSellerReviews(Integer sellerId);

    /**
     * 获取用户作为买家的评价
     */
    List<ReviewResponse> getBuyerReviews(Integer buyerId);

    /**
     * 获取商品平均评分
     */
    Double getProductAverageRating(Long productId);
}

