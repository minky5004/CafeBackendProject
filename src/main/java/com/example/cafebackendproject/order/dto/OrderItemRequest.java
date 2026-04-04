package com.example.cafebackendproject.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class OrderItemRequest {

    @NotNull(message = "메뉴 ID를 입력해주세요.")
    private Long menuId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;
}