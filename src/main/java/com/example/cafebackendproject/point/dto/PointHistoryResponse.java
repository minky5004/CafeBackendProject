package com.example.cafebackendproject.point.dto;

import com.example.cafebackendproject.domain.point.entity.PointHistory;
import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PointHistoryResponse {

    private final BigDecimal amount;
    private final PointHistoryType type;
    private final LocalDateTime createdAt;

    private PointHistoryResponse(BigDecimal amount, PointHistoryType type, LocalDateTime createdAt) {
        this.amount = amount;
        this.type = type;
        this.createdAt = createdAt;
    }

    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getAmount(),
                history.getType(),
                history.getCreatedAt()
        );
    }
}