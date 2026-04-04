package com.example.cafebackendproject.domain.order.repository;

import com.example.cafebackendproject.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 특정 사용자의 주문 내역 (최신순)
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}