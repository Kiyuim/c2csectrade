package lut.cn.c2cplatform.event;

import org.springframework.context.ApplicationEvent;

public class ProductDeletedEvent extends ApplicationEvent {
    private final Long productId;

    public ProductDeletedEvent(Object source, Long productId) {
        super(source);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
