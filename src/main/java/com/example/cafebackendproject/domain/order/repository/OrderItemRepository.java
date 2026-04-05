package com.example.cafebackendproject.domain.order.repository;

import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 최근 7일간 주문 수량 합산 기준 인기 메뉴
    @Query("SELECT oi.menu FROM OrderItem oi JOIN oi.order o " +
            "WHERE o.createdAt >= :since " +
            "GROUP BY oi.menu " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Menu> findPopularMenus(@Param("since") LocalDateTime since, Pageable pageable);
}
