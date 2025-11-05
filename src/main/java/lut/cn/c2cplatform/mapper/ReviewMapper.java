package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Insert("INSERT INTO review (order_id, product_id, buyer_id, seller_id, product_rating, seller_rating, comment, review_images, is_anonymous, created_at, updated_at) " +
            "VALUES (#{orderId}, #{productId}, #{buyerId}, #{sellerId}, #{productRating}, #{sellerRating}, #{comment}, #{reviewImages}, #{isAnonymous}, " +
            "COALESCE(#{createdAt}, CURRENT_TIMESTAMP), COALESCE(#{updatedAt}, CURRENT_TIMESTAMP))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Review review);

    @Select("SELECT * FROM review WHERE id = #{id}")
    Review selectById(Long id);

    @Select("SELECT * FROM review WHERE order_id = #{orderId}")
    Review selectByOrderId(Integer orderId);

    @Select("SELECT * FROM review WHERE product_id = #{productId} ORDER BY created_at DESC")
    List<Review> selectByProductId(Long productId);

    @Select("SELECT * FROM review WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<Review> selectBySellerId(Integer sellerId);

    @Select("SELECT * FROM review WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<Review> selectByBuyerId(Integer buyerId);

    @Select("SELECT AVG(product_rating) FROM review WHERE product_id = #{productId}")
    Double getAverageProductRating(Long productId);

    @Select("SELECT AVG(seller_rating) FROM review WHERE seller_id = #{sellerId}")
    Double getAverageSellerRating(Integer sellerId);

    @Select("SELECT COUNT(*) FROM review WHERE seller_id = #{sellerId} AND seller_rating >= 4")
    Integer countPositiveReviews(Integer sellerId);

    @Select("SELECT COUNT(*) FROM review WHERE seller_id = #{sellerId} AND seller_rating = 3")
    Integer countNeutralReviews(Integer sellerId);

    @Select("SELECT COUNT(*) FROM review WHERE seller_id = #{sellerId} AND seller_rating <= 2")
    Integer countNegativeReviews(Integer sellerId);
}

