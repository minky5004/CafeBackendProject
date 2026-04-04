package com.example.cafebackendproject.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PointChargeRequest {

    @NotNull(message = "충전 금액을 입력해주세요.")
    @Positive(message = "충전 금액은 0보다 커야 합니다.")
    private BigDecimal amount;
}
