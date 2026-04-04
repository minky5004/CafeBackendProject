package com.example.cafebackendproject.order.dto;

import com.example.cafebackendproject.domain.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class OrderStatusUpdateRequest {

    @NotNull(message = "변경할 주문 상태를 입력해주세요.")
    private OrderStatus status;
}