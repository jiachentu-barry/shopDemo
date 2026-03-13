package com.example.demo4.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo4.entity.*;
import com.example.demo4.repository.*;
import com.example.demo4.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public Order createOrder(Long userId, List<Long> productIds) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreateTimeDesc(userId);
        // 过滤出选中的商品
        List<CartItem> selected = cartItems.stream()
                .filter(ci -> productIds.contains(ci.getProductId()))
                .toList();

        if (selected.isEmpty()) {
            throw new RuntimeException("未选择任何商品");
        }

        // 生成订单号
        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;

        for (CartItem ci : selected) {
            Product product = productRepository.findById(ci.getProductId()).orElse(null);
            if (product == null) continue;

            // 检查库存
            int stock = product.getStock() != null ? product.getStock() : 0;
            if (stock < ci.getQuantity()) {
                throw new RuntimeException("商品【" + product.getName() + "】库存不足，当前库存: " + stock);
            }

            // 扣减库存
            product.setStock(stock - ci.getQuantity());
            productRepository.save(product);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductId(product.getId());
            oi.setProductName(product.getName());
            oi.setProductImage(product.getImagePath());
            oi.setPrice(product.getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            orderItems.add(oi);

            totalAmount = totalAmount.add(oi.getSubtotal());
            totalQty += ci.getQuantity();
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQty);
        orderRepository.save(order);

        // 从购物车移除已下单的商品
        for (Long pid : productIds) {
            cartItemRepository.deleteByUserIdAndProductId(userId, pid);
        }

        return order;
    }

    @Override
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Override
    public Order getOrderByNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo).orElse(null);
    }

    @Override
    @Transactional
    public Order payOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!"PENDING".equals(order.getStatus())) throw new RuntimeException("订单状态不正确");

        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(String orderNo, Long userId) {
        Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"PENDING".equals(order.getStatus())) throw new RuntimeException("只能取消待付款订单");

        order.setStatus("CANCELLED");

        // 恢复库存
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                Product product = productRepository.findById(oi.getProductId()).orElse(null);
                if (product != null) {
                    product.setStock((product.getStock() != null ? product.getStock() : 0) + oi.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        return orderRepository.save(order);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreateTimeDesc();
    }
}
