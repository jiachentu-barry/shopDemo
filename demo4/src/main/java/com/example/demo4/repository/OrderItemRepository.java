package com.example.demo4.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo4.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
