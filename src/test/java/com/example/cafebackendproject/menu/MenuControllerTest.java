package com.example.cafebackendproject.menu;

import com.example.cafebackendproject.RestDocsSupport;
import com.example.cafebackendproject.config.TestRedissonConfig;
import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.enums.MenuCategory;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.menu.controller.MenuController;
import com.example.cafebackendproject.menu.dto.MenuResponse;
import com.example.cafebackendproject.menu.service.MenuService;
import com.example.cafebackendproject.security.CustomUserDetails;
import com.example.cafebackendproject.security.SecurityConfig;
import com.example.cafebackendproject.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import({SecurityConfig.class, TestRedissonConfig.class})
@ActiveProfiles("test")
class MenuControllerTest extends RestDocsSupport {

    @MockBean
    private MenuService menuService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private Menu sampleMenu() {
        Menu menu = Menu.builder()
                .name("아메리카노").description("깔끔한 아메리카노")
                .price(new BigDecimal("4500")).category(MenuCategory.COFFEE)
                .build();
        ReflectionTestUtils.setField(menu, "id", 1L);
        return menu;
    }

    private CustomUserDetails adminDetails() {
        return new CustomUserDetails(1L, "admin@test.com", UserRole.ADMIN);
    }

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(1L, "test@test.com", UserRole.USER);
    }

    @Test
    @DisplayName("판매 중인 메뉴 목록 조회 성공")
    void getAvailableMenus_success() throws Exception {
        // given
        given(menuService.getAvailableMenus()).willReturn(List.of(MenuResponse.from(sampleMenu())));

        // when & then
        mockMvc.perform(get("/menus").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].name").value("아메리카노"))
                .andExpect(jsonPath("$.data[0].price").value(4500))
                .andDo(restDocsHandler("menu/list"));
    }

    @Test
    @DisplayName("인기 메뉴 조회 성공")
    void getPopularMenus_success() throws Exception {
        // given
        given(menuService.getPopularMenus()).willReturn(List.of(MenuResponse.from(sampleMenu())));

        // when & then
        mockMvc.perform(get("/menus/popular").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].name").value("아메리카노"))
                .andDo(restDocsHandler("menu/popular"));
    }

    @Test
    @DisplayName("메뉴 단건 조회 성공")
    void getMenu_success() throws Exception {
        // given
        given(menuService.getMenu(1L)).willReturn(MenuResponse.from(sampleMenu()));

        // when & then
        mockMvc.perform(get("/menus/1").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("아메리카노"))
                .andDo(restDocsHandler("menu/get"));
    }

    @Test
    @DisplayName("메뉴 등록 성공 - 관리자")
    void createMenu_success() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "아메리카노",
                    "description": "깔끔한 아메리카노",
                    "price": 4500,
                    "category": "COFFEE"
                }
                """;

        given(menuService.create(any())).willReturn(MenuResponse.from(sampleMenu()));

        // when & then
        mockMvc.perform(post("/menus")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("아메리카노"))
                .andExpect(jsonPath("$.data.category").value("COFFEE"))
                .andDo(restDocsHandler("menu/create"));
    }

    @Test
    @DisplayName("메뉴 등록 실패 - 권한 없음 (일반 사용자)")
    void createMenu_fail_forbidden() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "아메리카노",
                    "description": "깔끔한 아메리카노",
                    "price": 4500,
                    "category": "COFFEE"
                }
                """;

        // when & then
        mockMvc.perform(post("/menus")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("메뉴 등록 실패 - 이름 미입력")
    void createMenu_fail_blank_name() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "",
                    "price": 4500,
                    "category": "COFFEE"
                }
                """;

        // when & then
        mockMvc.perform(post("/menus")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("메뉴 수정 성공 - 관리자")
    void updateMenu_success() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "아메리카노 (변경)",
                    "description": "리뉴얼 아메리카노",
                    "price": 5000,
                    "category": "COFFEE",
                    "isAvailable": true
                }
                """;

        Menu updatedMenu = Menu.builder()
                .name("아메리카노 (변경)").description("리뉴얼 아메리카노")
                .price(new BigDecimal("5000")).category(MenuCategory.COFFEE)
                .build();
        ReflectionTestUtils.setField(updatedMenu, "id", 1L);

        given(menuService.update(eq(1L), any())).willReturn(MenuResponse.from(updatedMenu));

        // when & then
        mockMvc.perform(put("/menus/1")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("아메리카노 (변경)"))
                .andExpect(jsonPath("$.data.price").value(5000))
                .andDo(restDocsHandler("menu/update"));
    }

    @Test
    @DisplayName("메뉴 삭제 성공 - 관리자")
    void deleteMenu_success() throws Exception {
        // given
        willDoNothing().given(menuService).delete(1L);

        // when & then
        mockMvc.perform(delete("/menus/1").with(user(adminDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andDo(restDocsHandler("menu/delete"));
    }
}