package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.BuyNowRequest;
import lut.cn.c2cplatform.dto.PaymentRequest;
import lut.cn.c2cplatform.entity.Order;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping
    public ResponseEntity<Order> createOrder(Authentication authentication) {
        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        Order order = orderService.createOrderFromCart(user.getId().intValue());
        return ResponseEntity.ok(order);
    }

    @PostMapping("/buy-now")
    public ResponseEntity<Order> buyNow(
            @RequestBody BuyNowRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        Order order = orderService.createOrderForProduct(
                user.getId().intValue(),
                request.getProductId(),
                request.getQuantity()
        );
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(Authentication authentication) {
        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        List<Order> orders = orderService.getUserOrders(user.getId().intValue());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/seller")
    public ResponseEntity<List<Order>> getSellerOrders(Authentication authentication) {
        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        List<Order> orders = orderService.getSellerOrders(user.getId().intValue());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Integer orderId) {
        Order order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Order> payOrder(
            @PathVariable Integer orderId,
            @RequestBody PaymentRequest paymentRequest,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        Order order = orderService.payOrder(orderId, user.getId().intValue(), paymentRequest);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Order> confirmOrder(@PathVariable Integer orderId) {
        Order order = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Integer orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * 检查用户是否有未完成的订单（PENDING状态）
     */
    @GetMapping("/check-pending")
    public ResponseEntity<?> checkPendingOrders(Authentication authentication) {
        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        List<Order> orders = orderService.getUserOrders(user.getId().intValue());

        // 检查是否有待支付的订单
        boolean hasPendingOrder = orders.stream()
                .anyMatch(order -> "PENDING".equals(order.getStatus()));

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("hasPendingOrder", hasPendingOrder);

        if (hasPendingOrder) {
            Order pendingOrder = orders.stream()
                    .filter(order -> "PENDING".equals(order.getStatus()))
                    .findFirst()
                    .orElse(null);
            response.put("orderId", pendingOrder != null ? pendingOrder.getId() : null);
        }

        return ResponseEntity.ok(response);
    }
}
