package com.example.cafebackendproject.menu;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.enums.MenuCategory;
import com.example.cafebackendproject.domain.menu.repository.MenuRepository;
import com.example.cafebackendproject.domain.order.repository.OrderItemRepository;
import com.example.cafebackendproject.menu.dto.MenuCreateRequest;
import com.example.cafebackendproject.menu.dto.MenuResponse;
import com.example.cafebackendproject.menu.dto.MenuUpdateRequest;
import com.example.cafebackendproject.menu.service.MenuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @InjectMocks
    private MenuService menuService;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private Menu sampleMenu() {
        Menu menu = Menu.builder()
                .name("아메리카노").description("깔끔한 아메리카노")
                .price(new BigDecimal("4500")).category(MenuCategory.COFFEE)
                .build();
        ReflectionTestUtils.setField(menu, "id", 1L);
        return menu;
    }

    @Test
    @DisplayName("메뉴 등록 성공")
    void create_success() {
        // given
        MenuCreateRequest request = new MenuCreateRequest();
        ReflectionTestUtils.setField(request, "name", "아메리카노");
        ReflectionTestUtils.setField(request, "description", "깔끔한 아메리카노");
        ReflectionTestUtils.setField(request, "price", new BigDecimal("4500"));
        ReflectionTestUtils.setField(request, "category", MenuCategory.COFFEE);

        given(menuRepository.existsByNameAndDeletedFalse("아메리카노")).willReturn(false);
        given(menuRepository.save(any(Menu.class))).willReturn(sampleMenu());

        // when
        MenuResponse response = menuService.create(request);

        // then
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());

        assertThat(captor.getValue().getName()).isEqualTo("아메리카노");
        assertThat(captor.getValue().getCategory()).isEqualTo(MenuCategory.COFFEE);
        assertThat(response.getName()).isEqualTo("아메리카노");
    }

    @Test
    @DisplayName("메뉴 등록 실패 - 이름 중복")
    void create_fail_duplicate_name() {
        // given
        MenuCreateRequest request = new MenuCreateRequest();
        ReflectionTestUtils.setField(request, "name", "아메리카노");
        ReflectionTestUtils.setField(request, "price", new BigDecimal("4500"));
        ReflectionTestUtils.setField(request, "category", MenuCategory.COFFEE);

        given(menuRepository.existsByNameAndDeletedFalse("아메리카노")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> menuService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_MENU_NAME.getMessage());
    }

    @Test
    @DisplayName("판매 중인 메뉴 목록 조회 성공")
    void getAvailableMenus_success() {
        // given
        given(menuRepository.findAllByIsAvailableTrueAndDeletedFalse()).willReturn(List.of(sampleMenu()));

        // when
        List<MenuResponse> responses = menuService.getAvailableMenus();

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("아메리카노");
    }

    @Test
    @DisplayName("메뉴 단건 조회 성공")
    void getMenu_success() {
        // given
        given(menuRepository.findById(1L)).willReturn(Optional.of(sampleMenu()));

        // when
        MenuResponse response = menuService.getMenu(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("아메리카노");
    }

    @Test
    @DisplayName("메뉴 단건 조회 실패 - 존재하지 않는 메뉴")
    void getMenu_fail_not_found() {
        // given
        given(menuRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuService.getMenu(99L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.MENU_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("메뉴 수정 성공")
    void update_success() {
        // given
        MenuUpdateRequest request = new MenuUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "아메리카노 (변경)");
        ReflectionTestUtils.setField(request, "description", "리뉴얼");
        ReflectionTestUtils.setField(request, "price", new BigDecimal("5000"));
        ReflectionTestUtils.setField(request, "category", MenuCategory.COFFEE);
        ReflectionTestUtils.setField(request, "isAvailable", true);

        given(menuRepository.findById(1L)).willReturn(Optional.of(sampleMenu()));
        given(menuRepository.existsByNameAndDeletedFalse("아메리카노 (변경)")).willReturn(false);

        // when
        MenuResponse response = menuService.update(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("아메리카노 (변경)");
        assertThat(response.getPrice()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("메뉴 삭제 성공 (Soft Delete)")
    void delete_success() {
        // given
        Menu menu = sampleMenu();
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));

        // when
        menuService.delete(1L);

        // then
        assertThat(menu.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("메뉴 삭제 실패 - 존재하지 않는 메뉴")
    void delete_fail_not_found() {
        // given
        given(menuRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuService.delete(99L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.MENU_NOT_FOUND.getMessage());
    }
}