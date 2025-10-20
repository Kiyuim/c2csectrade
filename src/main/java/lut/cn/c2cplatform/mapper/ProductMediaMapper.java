package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.ProductMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMediaMapper {
    ProductMedia selectById(@Param("id") Long id);
    List<ProductMedia> selectByProductId(@Param("productId") Long productId);
    int insert(ProductMedia media);
    int update(ProductMedia media);
    int deleteById(@Param("id") Long id);
}

