package com.example.cafebackendproject.order.dto;

import com.example.cafebackendproject.domain.order.entity.OrderItem;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderItemResponse {

    private final Long menuId;
    private final String menuName;
    private final int quantity;
    private final BigDecimal price;

    private OrderItemResponse(Long menuId, String menuName, int quantity, BigDecimal price) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getMenu().getId(),
                item.getMenu().getName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}