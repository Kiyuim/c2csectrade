package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.BargainActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BargainActivityMapper {
    void insert(BargainActivity bargainActivity);
    BargainActivity selectById(Long id);
    List<BargainActivity> selectByUserId(@Param("userId") Long userId);
    List<BargainActivity> selectByProductId(@Param("productId") Long productId);
    void update(BargainActivity bargainActivity);
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    List<BargainActivity> selectExpiredActivities();

    // 检查用户是否已经为该商品发起过砍价
    BargainActivity selectActiveByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    // 将商品的所有活跃砍价活动标记为失败（商品被买走时调用）
    void markAllActiveAsFailed(@Param("productId") Long productId);
}

