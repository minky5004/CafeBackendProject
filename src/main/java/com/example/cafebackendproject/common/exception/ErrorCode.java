package com.example.cafebackendproject.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(400, "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다."),

    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(401, "비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),

    MENU_NOT_FOUND(404, "메뉴를 찾을 수 없습니다."),
    DUPLICATE_MENU_NAME(409, "이미 존재하는 메뉴 이름입니다."),

    INSUFFICIENT_BALANCE(400, "포인트 잔액이 부족합니다."),
    INVALID_AMOUNT(400, "충전 금액은 0보다 커야 합니다."),

    ORDER_NOT_FOUND(404, "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID(400, "이미 결제된 주문입니다."),
    ORDER_FORBIDDEN(403, "해당 주문에 대한 권한이 없습니다."),
    MENU_UNAVAILABLE(400, "현재 주문할 수 없는 메뉴가 포함되어 있습니다.");

    private final int status;
    private final String message;
}
