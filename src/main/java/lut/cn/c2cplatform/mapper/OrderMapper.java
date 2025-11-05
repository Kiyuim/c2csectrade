package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {
    void insert(Order order);
    Order findById(Integer id);
    void updateStatus(Order order);
    List<Order> findByUserId(@Param("userId") Integer userId);
    List<Order> findExpiredOrders();
    void batchUpdateStatusToExpired(@Param("orderIds") List<Integer> orderIds);

    @Select("SELECT DISTINCT o.* FROM `orders` o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "JOIN pms_product p ON oi.product_id = p.id " +
            "WHERE p.user_id = #{sellerId} " +
            "ORDER BY o.created_at DESC")
    List<Order> findBySellerId(@Param("sellerId") Integer sellerId);

    @Select("SELECT COUNT(DISTINCT o.id) FROM `orders` o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "JOIN pms_product p ON oi.product_id = p.id " +
            "WHERE p.user_id = #{sellerId} AND o.status = 'DELIVERED'")
    Integer countCompletedOrdersBySeller(Integer sellerId);

    @Select("SELECT COUNT(*) FROM `orders` WHERE user_id = #{buyerId} AND status = 'DELIVERED'")
    Integer countCompletedOrdersByBuyer(Integer buyerId);
}
