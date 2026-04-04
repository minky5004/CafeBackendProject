package com.example.cafebackendproject.order.controller;

import com.example.cafebackendproject.common.response.ApiResponse;
import com.example.cafebackendproject.order.dto.OrderCreateRequest;
import com.example.cafebackendproject.order.dto.OrderResponse;
import com.example.cafebackendproject.order.dto.OrderStatusUpdateRequest;
import com.example.cafebackendproject.order.service.OrderService;
import com.example.cafebackendproject.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.create(userDetails.getUserId(), request)));
    }

    // 전체 주문 조회 (관리자용)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    // 주문 상태 변경 (관리자용)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateStatus(orderId, request)));
    }

    // 결제
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<OrderResponse>> payment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.payment(userDetails.getUserId(), orderId)));
    }
}