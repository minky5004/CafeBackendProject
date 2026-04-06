package com.example.cafebackendproject.menu.service;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.repository.MenuRepository;
import com.example.cafebackendproject.domain.order.repository.OrderItemRepository;
import com.example.cafebackendproject.menu.dto.MenuCreateRequest;
import com.example.cafebackendproject.menu.dto.MenuResponse;
import com.example.cafebackendproject.menu.dto.MenuUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final OrderItemRepository orderItemRepository;

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

    @Transactional(readOnly = true)
    public List<MenuResponse> getAvailableMenus() {
        return menuRepository.findAllByIsAvailableTrueAndDeletedFalse().stream()
                .map(MenuResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAllByDeletedFalse().stream()
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

        if (!menu.getName().equals(request.getName()) && menuRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new CustomException(ErrorCode.DUPLICATE_MENU_NAME);
        }

        menu.update(request.getName(), request.getDescription(),
                request.getPrice(), request.getCategory(), request.isAvailable());

        return MenuResponse.from(menu);
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> getPopularMenus() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return orderItemRepository.findPopularMenus(since, PageRequest.of(0, 3)).stream()
                .map(MenuResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        menu.delete();
    }
}
