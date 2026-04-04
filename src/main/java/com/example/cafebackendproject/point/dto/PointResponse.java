package com.example.cafebackendproject.point.dto;

import com.example.cafebackendproject.domain.user.entity.User;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PointResponse {

    private final BigDecimal balance;

    private PointResponse(BigDecimal balance) {
        this.balance = balance;
    }

    public static PointResponse from(User user) {
        return new PointResponse(user.getBalance());
    }
}