package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ProductDTO;
import lut.cn.c2cplatform.service.FavoriteService;
import lut.cn.c2cplatform.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ProductService productService;

    @PostMapping("/add/{productId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long productId, Authentication authentication) {
        String username = authentication.getName();
        boolean success = favoriteService.addFavorite(username, productId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "收藏成功" : "收藏失败");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long productId, Authentication authentication) {
        String username = authentication.getName();
        boolean success = favoriteService.removeFavorite(username, productId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "取消收藏成功" : "取消收藏失败");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkFavorite(@PathVariable Long productId, Authentication authentication) {
        String username = authentication.getName();
        boolean isFavorite = favoriteService.isFavorite(username, productId);

        Map<String, Object> response = new HashMap<>();
        response.put("isFavorite", isFavorite);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFavorites(Authentication authentication) {
        String username = authentication.getName();
        List<Long> productIds = favoriteService.getFavoriteProductIds(username);

        // 获取商品详情
        List<ProductDTO> products = productIds.stream()
                .map(productService::getProductDTOById)
                .filter(product -> product != null)
                .toList();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getFavoriteCount(Authentication authentication) {
        String username = authentication.getName();
        int count = favoriteService.getFavoriteCount(username);

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }
}

