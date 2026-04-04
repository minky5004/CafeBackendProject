package com.example.cafebackendproject.auth.controller;

import com.example.cafebackendproject.auth.dto.LoginRequest;
import com.example.cafebackendproject.auth.dto.LoginResponse;
import com.example.cafebackendproject.auth.dto.RegisterRequest;
import com.example.cafebackendproject.auth.dto.RegisterResponse;
import com.example.cafebackendproject.auth.service.AuthService;
import com.example.cafebackendproject.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok()
                // 응답 헤더에도 토큰 포함
                .header("Authorization", "Bearer " + response.getAccessToken())
                .body(ApiResponse.success(response));
    }

    // JWT는 stateless이므로 클라이언트에서 토큰 삭제로 로그아웃 처리
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}