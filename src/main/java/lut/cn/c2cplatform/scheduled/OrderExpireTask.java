package lut.cn.c2cplatform.scheduled;

import lut.cn.c2cplatform.entity.CartItem;
import lut.cn.c2cplatform.entity.Order;
import lut.cn.c2cplatform.entity.OrderItem;
import lut.cn.c2cplatform.mapper.CartItemMapper;
import lut.cn.c2cplatform.mapper.OrderItemMapper;
import lut.cn.c2cplatform.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单过期自动关闭任务
 * 每分钟执行一次，检查并关闭过期的待支付订单
 * 如果订单来自购物车，则将商品恢复到购物车
 */
@Component
public class OrderExpireTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderExpireTask.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private lut.cn.c2cplatform.mapper.ProductMapper productMapper;

    /**
     * 每分钟执行一次，检查并关闭过期订单
     * 使用 @Scheduled 注解实现定时任务
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    @Transactional
    public void closeExpiredOrders() {
        try {
            // 查询所有过期的待支付订单
            List<Order> expiredOrders = orderMapper.findExpiredOrders();

            if (expiredOrders.isEmpty()) {
                return;
            }

            logger.info("发现 {} 个过期订单，开始处理...", expiredOrders.size());

            for (Order order : expiredOrders) {
                try {
                    // 获取订单项
                    List<OrderItem> orderItems = orderItemMapper.findByOrderId(order.getId());

                    // 恢复库存和购物车商品
                    for (OrderItem item : orderItems) {
                        // 恢复库存
                        try {
                            lut.cn.c2cplatform.entity.Product product = productMapper.selectById(item.getProductId().longValue());
                            if (product != null) {
                                product.setStock(product.getStock() + item.getQuantity());
                                product.setUpdatedAt(java.time.LocalDateTime.now());
                                productMapper.update(product);
                                logger.info("订单 {} 的商品 {} 库存已恢复 {} 件", order.getId(), item.getProductId(), item.getQuantity());
                            }
                        } catch (Exception e) {
                            logger.error("恢复商品 {} 库存失败: {}", item.getProductId(), e.getMessage());
                        }

                        // 恢复来自购物车的商品
                        if (Boolean.TRUE.equals(item.getFromCart())) {
                            // 检查购物车中是否已存在该商品
                            CartItem existingCartItem = cartItemMapper.selectByUserIdAndProductId(
                                order.getUserId().longValue(),
                                item.getProductId().longValue()
                            );

                            if (existingCartItem != null) {
                                // 如果购物车中已存在，更新数量
                                existingCartItem.setQuantity(existingCartItem.getQuantity() + item.getQuantity());
                                existingCartItem.setUpdatedAt(Instant.now());
                                cartItemMapper.update(existingCartItem);
                                logger.info("订单 {} 的商品 {} 已恢复到购物车（更新数量）", order.getId(), item.getProductId());
                            } else {
                                // 如果不存在，重新添加到购物车
                                CartItem cartItem = new CartItem();
                                cartItem.setUserId(order.getUserId().longValue());
                                cartItem.setProductId(item.getProductId().longValue());
                                cartItem.setQuantity(item.getQuantity());
                                cartItem.setCreatedAt(Instant.now());
                                cartItem.setUpdatedAt(Instant.now());
                                cartItemMapper.insert(cartItem);
                                logger.info("订单 {} 的商品 {} 已恢复到购物车（新增）", order.getId(), item.getProductId());
                            }
                        }
                    }

                    logger.info("订单 {} 已过期，库存和购物车已恢复", order.getId());
                } catch (Exception e) {
                    logger.error("处理过期订单 {} 失败: {}", order.getId(), e.getMessage(), e);
                }
            }

            // 批量更新订单状态为 EXPIRED
            List<Integer> orderIds = expiredOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());

            orderMapper.batchUpdateStatusToExpired(orderIds);

            logger.info("成功关闭 {} 个过期订单", orderIds.size());

        } catch (Exception e) {
            logger.error("关闭过期订单任务执行失败: {}", e.getMessage(), e);
        }
    }
}

