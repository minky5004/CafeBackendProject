package com.example.cafebackendproject.order.controller;

import com.example.cafebackendproject.common.response.ApiResponse;
import com.example.cafebackendproject.order.dto.OrderResponse;
import com.example.cafebackendproject.order.service.OrderService;
import com.example.cafebackendproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/orders")
public class UserOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(userDetails.getUserId())));
    }
}
