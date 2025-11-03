package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper {
    void insert(OrderItem orderItem);
    java.util.List<OrderItem> findByOrderId(Integer orderId);
}
