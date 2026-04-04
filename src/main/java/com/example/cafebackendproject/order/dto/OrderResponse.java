package com.example.cafebackendproject.order.dto;

import com.example.cafebackendproject.domain.order.entity.Order;
import com.example.cafebackendproject.domain.order.enums.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {

    private final Long orderId;
    private final List<OrderItemResponse> items;
    private final BigDecimal totalPrice;
    private final OrderStatus status;
    private final LocalDateTime createdAt;

    private OrderResponse(Long orderId, List<OrderItemResponse> items,
                          BigDecimal totalPrice, OrderStatus status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.items = items;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                items,
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}