package lut.cn.c2cplatform.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Event DTOs for Message Queue
 */
public class Events {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderPaidEvent implements Serializable {
        private Integer orderId;
        private Integer userId;
        private BigDecimal amount;
        private String paymentMethod;
        private Date paidAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCanceledEvent implements Serializable {
        private Integer orderId;
        private Integer userId;
        private String reason;
        private Date canceledAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCompletedEvent implements Serializable {
        private Integer orderId;
        private Integer userId;
        private Integer sellerId;
        private Date completedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCreatedEvent implements Serializable {
        private Long productId;
        private Long userId;
        private String productName;
        private String category;
        private Date createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockLowEvent implements Serializable {
        private Long productId;
        private String productName;
        private Integer currentStock;
        private Long sellerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationUpdateEvent implements Serializable {
        private Long productId;
        private String action; // "view", "favorite", "cart", "order", "review"
        private Long userId;
        private Date timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMessageEvent implements Serializable {
        private Integer userId;
        private String messageType; // "ORDER", "PAYMENT", "REVIEW", "SYSTEM"
        private String title;
        private String content;
        private Date timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailNotificationEvent implements Serializable {
        private String toEmail;
        private String subject;
        private String content;
        private String template;
        private Date timestamp;
    }
}

