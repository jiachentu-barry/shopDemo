package com.example.demo4.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo4.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findAllByOrderByCreateTimeDesc();
}
