package com.example.cafebackendproject.order.event;

import com.example.cafebackendproject.domain.order.entity.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class OrderEventPayload {

    private final Long orderId;
    private final Long userId;
    private final BigDecimal totalPrice;
    private final LocalDateTime paidAt;

    private OrderEventPayload(Long orderId, Long userId, BigDecimal totalPrice, LocalDateTime paidAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.paidAt = paidAt;
    }

    public static OrderEventPayload from(Order order) {
        return new OrderEventPayload(
                order.getId(),
                order.getUser().getId(),
                order.getTotalPrice(),
                LocalDateTime.now()
        );
    }
}