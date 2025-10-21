package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.entity.CartItem;
import lut.cn.c2cplatform.mapper.CartItemMapper;
import lut.cn.c2cplatform.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private UserMapper userMapper;

    public boolean addToCart(String username, Long productId, Integer quantity) {
        var user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }

        // 检查是否已在购物车
        CartItem existing = cartItemMapper.selectByUserIdAndProductId(user.getId(), productId);
        if (existing != null) {
            // 更新数量
            existing.setQuantity(existing.getQuantity() + quantity);
            existing.setUpdatedAt(Instant.now());
            cartItemMapper.update(existing);
        } else {
            // 新增
            CartItem cartItem = CartItem.builder()
                    .userId(user.getId())
                    .productId(productId)
                    .quantity(quantity)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            cartItemMapper.insert(cartItem);
        }

        return true;
    }

    public boolean updateCartItemQuantity(String username, Long productId, Integer quantity) {
        var user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }

        CartItem cartItem = cartItemMapper.selectByUserIdAndProductId(user.getId(), productId);
        if (cartItem == null) {
            return false;
        }

        cartItem.setQuantity(quantity);
        cartItem.setUpdatedAt(Instant.now());
        cartItemMapper.update(cartItem);
        return true;
    }

    public boolean removeFromCart(String username, Long productId) {
        var user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }

        cartItemMapper.deleteByUserIdAndProductId(user.getId(), productId);
        return true;
    }

    public List<CartItem> getCartItems(String username) {
        var user = userMapper.selectByUsername(username);
        if (user == null) {
            return List.of();
        }

        return cartItemMapper.selectByUserId(user.getId());
    }

    public int getCartCount(String username) {
        var user = userMapper.selectByUsername(username);
        if (user == null) {
            return 0;
        }

        return cartItemMapper.countByUserId(user.getId());
    }
}

