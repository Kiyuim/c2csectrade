package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {
    Product selectById(@Param("id") Long id);
    List<Product> selectAll();
    List<Product> selectByUserId(@Param("userId") Long userId);
    int insert(Product product);
    int update(Product product);
    int deleteById(@Param("id") Long id);
}

