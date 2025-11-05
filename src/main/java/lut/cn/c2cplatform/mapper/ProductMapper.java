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
    List<Product> selectWithFilters(@Param("keyword") String keyword,
                                     @Param("minPrice") java.math.BigDecimal minPrice,
                                     @Param("maxPrice") java.math.BigDecimal maxPrice,
                                     @Param("conditionLevel") Integer conditionLevel,
                                     @Param("location") String location,
                                     @Param("category") String category);
    int insert(Product product);
    int update(Product product);
    int deleteById(@Param("id") Long id);

    // For recommendations
    List<Product> selectByIds(@Param("ids") List<Long> ids);
    List<Product> selectByCategory(@Param("category") String category);
    List<Product> selectRecentProducts(@Param("limit") int limit);
    List<Product> selectAllActive(); // Get all active products for recommendation computation

    // 防止超卖：使用行锁查询商品
    Product selectByIdForUpdate(@Param("id") Long id);

    // 减少库存（返回影响的行数，如果为0说明库存不足）
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}

