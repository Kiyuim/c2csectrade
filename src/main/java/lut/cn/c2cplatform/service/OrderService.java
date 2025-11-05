package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.dto.PaymentRequest;
import lut.cn.c2cplatform.entity.Order;
import java.util.List;

public interface OrderService {
    Order createOrderFromCart(Integer userId);
    Order createOrderForProduct(Integer userId, Long productId, Integer quantity);
    Order createBargainOrder(Integer userId, Long productId, Integer quantity, java.math.BigDecimal customPrice);
    Order payOrder(Integer orderId, Integer userId, PaymentRequest paymentRequest);
    Order confirmOrder(Integer orderId);
    Order cancelOrder(Integer orderId);
    List<Order> getUserOrders(Integer userId);
    List<Order> getSellerOrders(Integer sellerId);
    Order getOrderById(Integer orderId);
}
