package com.example.demo4.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.example.demo4.entity.Order;
import com.example.demo4.entity.Users;
import com.example.demo4.service.OrderService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private Users getLoginUser(HttpSession session) {
        return (Users) session.getAttribute("loginUser");
    }

    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, List<Long>> body,
                                            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<Long> productIds = body.get("productIds");
        if (productIds == null || productIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择商品");
            return result;
        }
        try {
            Order order = orderService.createOrder(user.getId(), productIds);
            result.put("success", true);
            result.put("message", "下单成功");
            result.put("orderNo", order.getOrderNo());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listOrders(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<Order> orders = orderService.getUserOrders(user.getId());
        result.put("success", true);
        result.put("orders", orders);
        return result;
    }

    @GetMapping("/detail")
    public Map<String, Object> orderDetail(@RequestParam String orderNo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        Order order = orderService.getOrderByNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            result.put("success", false);
            result.put("message", "订单不存在");
            return result;
        }
        result.put("success", true);
        result.put("order", order);
        return result;
    }

    @PostMapping("/pay")
    public Map<String, Object> payOrder(@RequestParam String orderNo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        try {
            Order order = orderService.payOrder(orderNo);
            if (!order.getUserId().equals(user.getId())) {
                result.put("success", false);
                result.put("message", "无权操作");
                return result;
            }
            result.put("success", true);
            result.put("message", "支付成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancelOrder(@RequestParam String orderNo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        try {
            orderService.cancelOrder(orderNo, user.getId());
            result.put("success", true);
            result.put("message", "订单已取消");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
