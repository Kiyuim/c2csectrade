package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.document.ProductDocument;
import lut.cn.c2cplatform.dto.SearchRequestDTO;
import lut.cn.c2cplatform.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class SearchController {
    @Autowired
    private SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<List<ProductDocument>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) Integer conditionLevel,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            SearchRequestDTO request = new SearchRequestDTO();
            request.setKeyword(keyword);
            if (minPrice != null) {
                request.setMinPrice(new java.math.BigDecimal(minPrice));
            }
            if (maxPrice != null) {
                request.setMaxPrice(new java.math.BigDecimal(maxPrice));
            }
            request.setConditionLevel(conditionLevel);
            request.setLocation(location);
            request.setPage(page);
            request.setSize(size);

            List<ProductDocument> results = searchService.searchProducts(request);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncAllProducts() {
        try {
            searchService.syncAllProducts();
            return ResponseEntity.ok("Successfully synced all products to Elasticsearch");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to sync products: " + e.getMessage());
        }
    }
}

