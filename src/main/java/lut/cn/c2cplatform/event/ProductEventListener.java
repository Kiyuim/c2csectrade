package lut.cn.c2cplatform.event;

import lut.cn.c2cplatform.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    @Autowired
    private SearchService searchService;

    @Async
    @EventListener
    public void handleProductCreated(ProductCreatedEvent event) {
        try {
            searchService.indexProduct(event.getProduct());
        } catch (Exception e) {
            System.err.println("Failed to index product to Elasticsearch: " + e.getMessage());
        }
    }
}

