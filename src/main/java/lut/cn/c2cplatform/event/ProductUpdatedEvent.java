package lut.cn.c2cplatform.event;

import lut.cn.c2cplatform.entity.Product;
import org.springframework.context.ApplicationEvent;

public class ProductUpdatedEvent extends ApplicationEvent {
    private final Product product;

    public ProductUpdatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
