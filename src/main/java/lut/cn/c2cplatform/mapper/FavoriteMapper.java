package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {
    void insert(Favorite favorite);

    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    Favorite selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<Long> selectProductIdsByUserId(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);
}

