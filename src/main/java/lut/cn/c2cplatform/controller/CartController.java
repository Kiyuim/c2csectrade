package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ProductDTO;
import lut.cn.c2cplatform.entity.CartItem;
import lut.cn.c2cplatform.service.CartService;
import lut.cn.c2cplatform.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> request, Authentication authentication) {
        String username = authentication.getName();
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = request.containsKey("quantity") ?
                Integer.valueOf(request.get("quantity").toString()) : 1;

        boolean success = cartService.addToCart(username, productId, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "已加入购物车" : "加入购物车失败");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCartItem(@RequestBody Map<String, Object> request, Authentication authentication) {
        String username = authentication.getName();
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        boolean success = cartService.updateCartItemQuantity(username, productId, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "数量已更新" : "更新失败");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long productId, Authentication authentication) {
        String username = authentication.getName();
        boolean success = cartService.removeFromCart(username, productId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "已从购物车移除" : "移除失败");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCartItems(Authentication authentication) {
        String username = authentication.getName();
        List<CartItem> cartItems = cartService.getCartItems(username);

        // 组装商品信息
        List<Map<String, Object>> result = cartItems.stream()
                .map(item -> {
                    ProductDTO product = productService.getProductDTOById(item.getProductId());
                    if (product != null) {
                        Map<String, Object> cartItemData = new HashMap<>();
                        cartItemData.put("cartItemId", item.getId());
                        cartItemData.put("quantity", item.getQuantity());
                        cartItemData.put("product", product);
                        return cartItemData;
                    }
                    return null;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCartCount(Authentication authentication) {
        String username = authentication.getName();
        int count = cartService.getCartCount(username);

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }
}

