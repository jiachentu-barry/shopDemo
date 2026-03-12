package com.example.demo4.service;

import java.util.List;

import com.example.demo4.entity.Order;

public interface OrderService {
    Order createOrder(Long userId, List<Long> productIds);

    List<Order> getUserOrders(Long userId);

    Order getOrderByNo(String orderNo);

    Order payOrder(String orderNo);

    Order cancelOrder(String orderNo, Long userId);
}
