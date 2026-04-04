package com.example.cafebackendproject.point.controller;

import com.example.cafebackendproject.common.response.ApiResponse;
import com.example.cafebackendproject.point.dto.PointChargeRequest;
import com.example.cafebackendproject.point.dto.PointHistoryResponse;
import com.example.cafebackendproject.point.dto.PointResponse;
import com.example.cafebackendproject.point.service.PointService;
import com.example.cafebackendproject.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/point")
public class PointController {

    private final PointService pointService;

    @GetMapping
    public ResponseEntity<ApiResponse<PointResponse>> getBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(pointService.getBalance(userDetails.getUserId())));
    }

    @PatchMapping("/charge")
    public ResponseEntity<ApiResponse<PointResponse>> charge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PointChargeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(pointService.charge(userDetails.getUserId(), request)));
    }

    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(pointService.getHistories(userDetails.getUserId())));
    }
}