package com.example.demo4.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.example.demo4.entity.CartItem;
import com.example.demo4.entity.Users;
import com.example.demo4.service.CartService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private Users getLoginUser(HttpSession session) {
        return (Users) session.getAttribute("loginUser");
    }

    @PostMapping("/add")
    public Map<String, Object> addToCart(@RequestParam Long productId,
                                         @RequestParam(defaultValue = "1") Integer quantity,
                                         HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        cartService.addToCart(user.getId(), productId, quantity);
        result.put("success", true);
        result.put("message", "已加入购物车");
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listCart(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<CartItem> items = cartService.getCartItems(user.getId());
        result.put("success", true);
        result.put("items", items);
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateQuantity(@RequestParam Long productId,
                                               @RequestParam Integer quantity,
                                               HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        if (quantity < 1) {
            result.put("success", false);
            result.put("message", "数量不能小于1");
            return result;
        }
        CartItem item = cartService.updateQuantity(user.getId(), productId, quantity);
        if (item == null) {
            result.put("success", false);
            result.put("message", "购物车中无此商品");
            return result;
        }
        result.put("success", true);
        result.put("message", "已更新");
        return result;
    }

    @DeleteMapping("/remove")
    public Map<String, Object> removeFromCart(@RequestParam Long productId,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        cartService.removeFromCart(user.getId(), productId);
        result.put("success", true);
        result.put("message", "已移除");
        return result;
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clearCart(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        cartService.clearCart(user.getId());
        result.put("success", true);
        result.put("message", "购物车已清空");
        return result;
    }
}
