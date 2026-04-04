package com.example.cafebackendproject.domain.order.enums;

public enum OrderStatus {
    PENDING,    // 주문 생성 (미결제)
    PAID,       // 결제 완료
    PREPARING,  // 제조 중
    COMPLETED,  // 제조 완료
    CANCELLED   // 취소
}