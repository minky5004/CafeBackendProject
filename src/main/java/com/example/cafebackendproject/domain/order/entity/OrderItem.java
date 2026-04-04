package com.example.cafebackendproject.domain.order.entity;

import com.example.cafebackendproject.domain.menu.entity.Menu;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false)
    private int quantity;

    // 주문 시점의 가격을 저장 (이후 메뉴 가격 변경에 영향받지 않음)
    @Column(nullable = false)
    private BigDecimal price;

    public static OrderItem of(Order order, Menu menu, int quantity) {
        return OrderItem.builder()
                .order(order)
                .menu(menu)
                .quantity(quantity)
                .price(menu.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .build();
    }
}