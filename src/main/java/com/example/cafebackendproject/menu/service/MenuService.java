package com.example.cafebackendproject.menu.service;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.repository.MenuRepository;
import com.example.cafebackendproject.menu.dto.MenuCreateRequest;
import com.example.cafebackendproject.menu.dto.MenuResponse;
import com.example.cafebackendproject.menu.dto.MenuUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional
    public MenuResponse create(MenuCreateRequest request) {
        if (menuRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new CustomException(ErrorCode.DUPLICATE_MENU_NAME);
        }

        Menu menu = Menu.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .build();

        return MenuResponse.from(menuRepository.save(menu));
    }

    // 삭제되지 않고 판매 중인 메뉴 전체 조회
    @Transactional(readOnly = true)
    public List<MenuResponse> getAvailableMenus() {
        return menuRepository.findAllByIsAvailableTrueAndDeletedFalse().stream()
                .map(MenuResponse::from)
                .toList();
    }

    // 메뉴 전체 조회 (관리자용)
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAll().stream()
                .map(MenuResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        return MenuResponse.from(menu);
    }

    @Transactional
    public MenuResponse update(Long menuId, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        // 이름 변경 시 중복 체크 (자기 자신 제외)
        if (!menu.getName().equals(request.getName()) && menuRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new CustomException(ErrorCode.DUPLICATE_MENU_NAME);
        }

        menu.update(request.getName(), request.getDescription(),
                request.getPrice(), request.getCategory(), request.isAvailable());

        return MenuResponse.from(menu);
    }

    // Soft Delete: deleted = true, deletedAt 기록
    @Transactional
    public void delete(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        menu.delete();
    }
}