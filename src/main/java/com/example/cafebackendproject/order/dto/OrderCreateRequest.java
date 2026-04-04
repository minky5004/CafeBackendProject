package com.example.cafebackendproject.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCreateRequest {

    @NotEmpty(message = "주문 항목을 입력해주세요.")
    @Valid
    private List<OrderItemRequest> items;
}