package lut.cn.c2cplatform.event;

import lut.cn.c2cplatform.config.RabbitMQConfig;
import lut.cn.c2cplatform.service.HybridRecommendationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Event Consumers for processing async events
 */
@Component
public class EventConsumer {

    @Autowired
    private HybridRecommendationService hybridRecommendationService;

    /**
     * Process order paid events
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void handleOrderPaid(Events.OrderPaidEvent event) {
        try {
            System.out.println("Processing OrderPaidEvent: " + event.getOrderId());

            // Send notification to user
            // TODO: Implement notification service

            // Log for analytics
            System.out.println("Order " + event.getOrderId() + " paid by user " + event.getUserId());
        } catch (Exception e) {
            System.err.println("Error processing OrderPaidEvent: " + e.getMessage());
            // Could implement retry logic or dead letter queue here
        }
    }

    /**
     * Process order canceled events
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELED_QUEUE)
    public void handleOrderCanceled(Events.OrderCanceledEvent event) {
        try {
            System.out.println("Processing OrderCanceledEvent: " + event.getOrderId());

            // Send notification to user
            // TODO: Implement notification service

        } catch (Exception e) {
            System.err.println("Error processing OrderCanceledEvent: " + e.getMessage());
        }
    }

    /**
     * Process order completed events
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_COMPLETED_QUEUE)
    public void handleOrderCompleted(Events.OrderCompletedEvent event) {
        try {
            System.out.println("Processing OrderCompletedEvent: " + event.getOrderId());

            // Send notification to buyer and seller
            // TODO: Implement notification service

            // Trigger recommendation update
            // Order completion is a strong signal

        } catch (Exception e) {
            System.err.println("Error processing OrderCompletedEvent: " + e.getMessage());
        }
    }

    /**
     * Process product created events
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_CREATED_QUEUE)
    public void handleProductCreated(Events.ProductCreatedEvent event) {
        try {
            System.out.println("Processing ProductCreatedEvent: " + event.getProductId());

            // Index to Elasticsearch
            // TODO: Implement Elasticsearch indexing

            // Notify followers if seller has followers
            // TODO: Implement follower notification

        } catch (Exception e) {
            System.err.println("Error processing ProductCreatedEvent: " + e.getMessage());
        }
    }

    /**
     * Process product stock low events
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_STOCK_LOW_QUEUE)
    public void handleProductStockLow(Events.ProductStockLowEvent event) {
        try {
            System.out.println("Processing ProductStockLowEvent: " + event.getProductId());

            // Notify seller about low stock
            // TODO: Implement notification service

            System.out.println("Low stock alert for product " + event.getProductId() +
                             ": " + event.getCurrentStock() + " remaining");
        } catch (Exception e) {
            System.err.println("Error processing ProductStockLowEvent: " + e.getMessage());
        }
    }

    /**
     * Process recommendation update events
     */
    @RabbitListener(queues = RabbitMQConfig.RECOMMENDATION_UPDATE_QUEUE)
    public void handleRecommendationUpdate(Events.RecommendationUpdateEvent event) {
        try {
            // Update real-time popularity scores
            if (hybridRecommendationService != null) {
                hybridRecommendationService.updatePopularity(event.getProductId(), event.getAction());
            }
        } catch (Exception e) {
            System.err.println("Error processing RecommendationUpdateEvent: " + e.getMessage());
        }
    }

    /**
     * Process system message events
     */
    @RabbitListener(queues = RabbitMQConfig.SYSTEM_MESSAGE_QUEUE)
    public void handleSystemMessage(Events.SystemMessageEvent event) {
        try {
            System.out.println("Processing SystemMessageEvent for user: " + event.getUserId());

            // Store message in database
            // TODO: Implement message storage

            // Send via WebSocket if user is online
            // TODO: Implement WebSocket notification

        } catch (Exception e) {
            System.err.println("Error processing SystemMessageEvent: " + e.getMessage());
        }
    }

    /**
     * Process email notification events
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_NOTIFICATION_QUEUE)
    public void handleEmailNotification(Events.EmailNotificationEvent event) {
        try {
            System.out.println("Processing EmailNotificationEvent to: " + event.getToEmail());

            // Send email using email service
            // TODO: Implement email service

            System.out.println("Email sent to " + event.getToEmail() + ": " + event.getSubject());
        } catch (Exception e) {
            System.err.println("Error processing EmailNotificationEvent: " + e.getMessage());
        }
    }
}

