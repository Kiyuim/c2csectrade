package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.CreditScore;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CreditScoreMapper {

    @Insert("INSERT INTO credit_score (user_id, total_score, level, total_sales, total_purchases, average_seller_rating, positive_reviews, neutral_reviews, negative_reviews, updated_at) " +
            "VALUES (#{userId}, #{totalScore}, #{level}, #{totalSales}, #{totalPurchases}, #{averageSellerRating}, #{positiveReviews}, #{neutralReviews}, #{negativeReviews}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CreditScore creditScore);

    @Select("SELECT * FROM credit_score WHERE user_id = #{userId}")
    CreditScore selectByUserId(Long userId);

    @Update("UPDATE credit_score SET total_score = #{totalScore}, level = #{level}, total_sales = #{totalSales}, " +
            "total_purchases = #{totalPurchases}, average_seller_rating = #{averageSellerRating}, " +
            "positive_reviews = #{positiveReviews}, neutral_reviews = #{neutralReviews}, " +
            "negative_reviews = #{negativeReviews}, updated_at = #{updatedAt} WHERE user_id = #{userId}")
    void update(CreditScore creditScore);

    @Select("SELECT COUNT(*) FROM credit_score WHERE user_id = #{userId}")
    int existsByUserId(Long userId);
}

