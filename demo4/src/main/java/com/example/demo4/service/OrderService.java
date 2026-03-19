package com.example.demo4.service;

import java.time.LocalDate;
import java.util.List;

import com.example.demo4.entity.ExpiredOrderReport;
import com.example.demo4.entity.Order;

public interface OrderService {
    Order createOrder(Long userId, List<Long> productIds);

    List<Order> getUserOrders(Long userId);

    Order getOrderByNo(String orderNo);

    Order payOrder(String orderNo);

    Order cancelOrder(String orderNo, Long userId);

    List<Order> getAllOrders();

    int autoCancelExpiredOrders();

    ExpiredOrderReport generateDailyExpiredReport();

    ExpiredOrderReport generateExpiredReportByDate(LocalDate reportDate);

    List<Order> getPaidOrdersByDate(LocalDate reportDate);

    List<ExpiredOrderReport> getAllExpiredReports();
}
