package com.example.cafebackendproject.domain.order.entity;

import com.example.cafebackendproject.common.entity.CreatableEntity;
import com.example.cafebackendproject.domain.order.enums.OrderStatus;
import com.example.cafebackendproject.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 주문 생성 시 함께 저장
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public static Order of(User user, BigDecimal totalPrice) {
        return Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .build();
    }
}