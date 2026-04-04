package com.example.cafebackendproject.menu.controller;

import com.example.cafebackendproject.common.response.ApiResponse;
import com.example.cafebackendproject.menu.dto.MenuCreateRequest;
import com.example.cafebackendproject.menu.dto.MenuResponse;
import com.example.cafebackendproject.menu.dto.MenuUpdateRequest;
import com.example.cafebackendproject.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    // 관리자만 메뉴 등록 가능
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(menuService.create(request)));
    }

    // 누구나 판매 중인 메뉴 조회 가능
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAvailableMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAvailableMenus()));
    }

    // 관리자는 품절 포함 전체 조회
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAllMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllMenus()));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenu(menuId)));
    }

    // 관리자만 수정/삭제 가능
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> update(@PathVariable Long menuId,
                                                            @Valid @RequestBody MenuUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(menuService.update(menuId, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}