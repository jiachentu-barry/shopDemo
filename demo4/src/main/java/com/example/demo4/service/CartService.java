package com.example.demo4.service;

import java.util.List;

import com.example.demo4.entity.CartItem;

public interface CartService {
    CartItem addToCart(Long userId, Long productId, Integer quantity);

    List<CartItem> getCartItems(Long userId);

    CartItem updateQuantity(Long userId, Long productId, Integer quantity);

    void removeFromCart(Long userId, Long productId);

    void clearCart(Long userId);
}
