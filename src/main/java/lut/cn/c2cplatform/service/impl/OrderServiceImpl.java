package lut.cn.c2cplatform.service.impl;

import lut.cn.c2cplatform.dto.PaymentRequest;
import lut.cn.c2cplatform.entity.*;
import lut.cn.c2cplatform.mapper.*;
import lut.cn.c2cplatform.service.CreditScoreService;
import lut.cn.c2cplatform.service.OrderService;
import lut.cn.c2cplatform.service.HybridRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private lut.cn.c2cplatform.mapper.UserMapper userMapper;

    @Autowired
    private lut.cn.c2cplatform.mapper.BargainActivityMapper bargainActivityMapper;

    @Autowired
    private CreditScoreService creditScoreService;

    @Autowired(required = false)
    private HybridRecommendationService hybridRecommendationService;

    @Override
    @Transactional
    public Order createOrderFromCart(Integer userId) {
        // 自动取消该用户所有过期的PENDING订单
        cancelExpiredPendingOrders(userId);

        List<CartItem> cartItems = cartItemMapper.selectByUserId(userId.longValue());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 先锁定所有商品并检查库存
        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectByIdForUpdate(cartItem.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在: " + cartItem.getProductId());
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("商品【" + product.getName() + "】库存不足，当前库存：" + product.getStock() + "件");
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }

        // 注意：不在创建订单时扣减库存，在支付成功后才扣减
        // 这样避免未支付订单占用库存，也避免错误地标记砍价活动失败

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");

        // 设置订单过期时间为15分钟后
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(java.util.Calendar.MINUTE, 15);
        order.setExpireTime(calendar.getTime());

        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        orderMapper.insert(order);

        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId().intValue());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setFromCart(true); // 标记来自购物车
            orderItemMapper.insert(orderItem);
        }

        // 暂时不删除购物车，等支付成功或订单过期后再处理
        // cartItemMapper.deleteByUserId(userId.longValue());

        return order;
    }

    @Override
    @Transactional
    public Order createOrderForProduct(Integer userId, Long productId, Integer quantity) {
        // 自动取消该用户所有过期的PENDING订单
        cancelExpiredPendingOrders(userId);

        // 使用行锁查询商品（防止并发超卖）
        Product product = productMapper.selectByIdForUpdate(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查库存（但不扣减，支付成功后才扣减）
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足，当前库存：" + product.getStock() + "件");
        }

        // 注意：不在创建订单时扣减库存，在支付成功后才扣减
        // 这样避免未支付订单占用库存，也避免错误地标记砍价活动失败

        // 计算总价
        BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(quantity));

        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");

        // 设置订单过期时间为15分钟后
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(java.util.Calendar.MINUTE, 15);
        order.setExpireTime(calendar.getTime());

        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        orderMapper.insert(order);

        // 创建订单项
        OrderItem newOrderItem = new OrderItem();
        newOrderItem.setOrderId(order.getId());
        newOrderItem.setProductId(product.getId().intValue());
        newOrderItem.setQuantity(quantity);
        newOrderItem.setPrice(product.getPrice());
        newOrderItem.setFromCart(false); // 标记不来自购物车（直接购买）
        orderItemMapper.insert(newOrderItem);

        return order;
    }

    @Override
    @Transactional
    public Order createBargainOrder(Integer userId, Long productId, Integer quantity, BigDecimal customPrice) {
        // 使用行锁查询商品（防止并发超卖）
        Product product = productMapper.selectByIdForUpdate(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查库存（但不扣减，支付成功后才扣减）
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足，当前库存：" + product.getStock() + "件");
        }

        // 注意：不在创建订单时扣减库存，在支付成功后才扣减
        // 这样避免未支付订单占用库存，也避免错误地标记砍价活动失败

        // 使用砍价价格作为总价
        BigDecimal totalAmount = customPrice.multiply(new BigDecimal(quantity));

        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");

        // 设置订单过期时间为15分钟后
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(java.util.Calendar.MINUTE, 15);
        order.setExpireTime(calendar.getTime());

        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        orderMapper.insert(order);

        // 创建订单项，使用砍价价格
        OrderItem newOrderItem = new OrderItem();
        newOrderItem.setOrderId(order.getId());
        newOrderItem.setProductId(product.getId().intValue());
        newOrderItem.setQuantity(quantity);
        newOrderItem.setPrice(customPrice); // 使用砍价价格
        newOrderItem.setFromCart(false); // 标记不来自购物车（砍价购买）
        orderItemMapper.insert(newOrderItem);

        return order;
    }

    @Override
    @Transactional
    public Order payOrder(Integer orderId, Integer userId, PaymentRequest paymentRequest) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Order not found or not pending");
        }

        // 验证订单所属用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to pay this order");
        }

        // 验证支付密码格式
        if (paymentRequest.getPassword() == null || !paymentRequest.getPassword().matches("\\d{6}")) {
            throw new RuntimeException("支付密码必须是6位数字");
        }

        // 从数据库获取用户信息并验证支付密码
        lut.cn.c2cplatform.entity.User user = userMapper.selectById(userId.longValue());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 获取订单项，删除来自购物车的商品
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            if (Boolean.TRUE.equals(item.getFromCart())) {
                // 从购物车中删除该商品
                cartItemMapper.deleteByUserIdAndProductId(userId.longValue(), item.getProductId().longValue());
            }
        }

        // 检查用户是否已设置支付密码
        if (user.getPaymentPasswordHash() == null || user.getPaymentPasswordHash().trim().isEmpty()) {
            throw new RuntimeException("请先设置支付密码");
        }

        // 使用BCrypt验证支付密码
        if (!org.springframework.security.crypto.bcrypt.BCrypt.checkpw(
                paymentRequest.getPassword(),
                user.getPaymentPasswordHash())) {
            throw new RuntimeException("支付密码错误");
        }

        // 支付成功前，先扣减库存（重要：只有支付成功才扣减库存）
        for (OrderItem item : orderItems) {
            Long productId = item.getProductId().longValue();
            Integer quantity = item.getQuantity();

            // 使用行锁查询商品
            Product product = productMapper.selectByIdForUpdate(productId);
            if (product == null) {
                throw new RuntimeException("商品不存在");
            }

            // Update product popularity when order is paid
            if (hybridRecommendationService != null) {
                hybridRecommendationService.updatePopularity(productId, "order");
            }

            // 检查库存
            if (product.getStock() < quantity) {
                throw new RuntimeException("商品【" + product.getName() + "】库存不足，当前库存：" + product.getStock() + "件");
            }

            // 扣减库存
            int updated = productMapper.decreaseStock(productId, quantity);
            if (updated == 0) {
                throw new RuntimeException("商品【" + product.getName() + "】库存扣减失败，可能已被抢购");
            }

            // 检查扣减后的库存，如果为0则标记所有该商品的砍价活动为失败
            Product updatedProduct = productMapper.selectById(productId);
            if (updatedProduct != null && updatedProduct.getStock() == 0) {
                // 商品已售罄，将所有活跃的砍价活动标记为失败
                bargainActivityMapper.markAllActiveAsFailed(productId);
            }
        }

        // 如果使用余额支付，检查余额是否足够并扣除余额
        if ("balance".equals(paymentRequest.getPaymentMethod())) {
            java.math.BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : java.math.BigDecimal.ZERO;

            // 检查余额是否足够
            if (currentBalance.compareTo(order.getTotalAmount()) < 0) {
                throw new RuntimeException("余额不足，当前余额：¥" + currentBalance.toPlainString() +
                                         "，需要支付：¥" + order.getTotalAmount().toPlainString());
            }

            // 扣除余额
            user.setBalance(currentBalance.subtract(order.getTotalAmount()));
            user.setUpdatedAt(java.time.Instant.now());
            userMapper.update(user);
        }

        order.setStatus("PAID");
        order.setPaymentMethod(paymentRequest.getPaymentMethod());
        order.setUpdatedAt(new Date());
        orderMapper.updateStatus(order);

        Transaction transaction = new Transaction();
        transaction.setOrderId(orderId);
        transaction.setAmount(order.getTotalAmount());
        transaction.setTransactionType("PAYMENT");
        transaction.setPaymentMethod(paymentRequest.getPaymentMethod());
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(new Date());
        transactionMapper.insert(transaction);

        return order;
    }

    @Override
    @Transactional
    public Order confirmOrder(Integer orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("只有已支付的订单才能确认收货");
        }

        // 获取订单项，找到所有卖家并增加余额
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId().longValue());
            if (product != null) {
                // 获取卖家信息
                lut.cn.c2cplatform.entity.User seller = userMapper.selectById(product.getUserId());
                if (seller != null) {
                    // 计算该商品的总价
                    java.math.BigDecimal itemTotal = item.getPrice().multiply(new java.math.BigDecimal(item.getQuantity()));

                    // 验证金额是否有效
                    if (itemTotal.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        throw new RuntimeException("订单金额异常，无法完成确认收货");
                    }

                    // 增加卖家余额
                    java.math.BigDecimal currentBalance = seller.getBalance() != null ? seller.getBalance() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal newBalance = currentBalance.add(itemTotal);

                    // 验证余额不会超过限制 (DECIMAL(15,2) 最大值)
                    java.math.BigDecimal maxBalance = new java.math.BigDecimal("9999999999999.99");
                    if (newBalance.compareTo(maxBalance) > 0) {
                        throw new RuntimeException("余额超出系统限制，请联系管理员");
                    }

                    seller.setBalance(newBalance);
                    seller.setUpdatedAt(java.time.Instant.now());
                    userMapper.update(seller);

                    // 更新卖家信用分
                    creditScoreService.updateCreditScore(product.getUserId());
                }
            }
        }

        // 更新买家信用分
        creditScoreService.updateCreditScore(order.getUserId().longValue());

        order.setStatus("COMPLETED");
        order.setUpdatedAt(new Date());
        orderMapper.updateStatus(order);

        return order;
    }

    @Override
    @Transactional
    public Order cancelOrder(Integer orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Order not found or not pending");
        }

        // 获取订单项
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);

        // 注意：因为创建订单时没有扣减库存，所以取消订单时也不需要恢复库存
        // 只处理购物车恢复
        for (OrderItem item : orderItems) {

            // 如果商品来自购物车，恢复到购物车
            if (Boolean.TRUE.equals(item.getFromCart())) {
                CartItem existingCartItem = cartItemMapper.selectByUserIdAndProductId(
                    order.getUserId().longValue(),
                    item.getProductId().longValue()
                );

                if (existingCartItem != null) {
                    // 购物车中已存在，更新数量
                    existingCartItem.setQuantity(existingCartItem.getQuantity() + item.getQuantity());
                    existingCartItem.setUpdatedAt(java.time.Instant.now());
                    cartItemMapper.update(existingCartItem);
                } else {
                    // 购物车中不存在，新增
                    CartItem cartItem = new CartItem();
                    cartItem.setUserId(order.getUserId().longValue());
                    cartItem.setProductId(item.getProductId().longValue());
                    cartItem.setQuantity(item.getQuantity());
                    cartItem.setCreatedAt(java.time.Instant.now());
                    cartItem.setUpdatedAt(java.time.Instant.now());
                    cartItemMapper.insert(cartItem);
                }
            }
        }

        // 更新订单状态
        order.setStatus("CANCELED");
        order.setUpdatedAt(new Date());
        orderMapper.updateStatus(order);

        return order;
    }

    @Override
    public List<Order> getUserOrders(Integer userId) {
        return orderMapper.findByUserId(userId);
    }

    @Override
    public List<Order> getSellerOrders(Integer sellerId) {
        return orderMapper.findBySellerId(sellerId);
    }

    @Override
    public Order getOrderById(Integer orderId) {
        return orderMapper.findById(orderId);
    }

    /**
     * 自动取消用户所有过期的PENDING订单
     * 在创建新订单前调用，避免重复创建订单
     */
    private void cancelExpiredPendingOrders(Integer userId) {
        List<Order> userOrders = orderMapper.findByUserId(userId);
        Date now = new Date();

        for (Order order : userOrders) {
            // 如果订单是PENDING状态且已过期，自动取消
            if ("PENDING".equals(order.getStatus()) && order.getExpireTime() != null && order.getExpireTime().before(now)) {
                try {
                    // 使用现有的取消订单逻辑
                    cancelOrder(order.getId());
                } catch (Exception e) {
                    // 如果取消失败，记录日志但继续处理（不影响新订单创建）
                    System.err.println("自动取消过期订单失败: " + order.getId() + ", 错误: " + e.getMessage());
                }
            }
        }
    }
}
