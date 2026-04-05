package com.example.cafebackendproject.point;

import com.example.cafebackendproject.RestDocsSupport;
import com.example.cafebackendproject.config.TestRedissonConfig;
import com.example.cafebackendproject.domain.point.entity.PointHistory;
import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.point.controller.PointController;
import com.example.cafebackendproject.point.dto.PointHistoryResponse;
import com.example.cafebackendproject.point.dto.PointResponse;
import com.example.cafebackendproject.point.service.PointService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import({SecurityConfig.class, TestRedissonConfig.class})
@ActiveProfiles("test")
class PointControllerTest extends RestDocsSupport {

    @MockBean
    private PointService pointService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final CustomUserDetails userDetails =
            new CustomUserDetails(1L, "test@test.com", UserRole.USER);

    @Test
    @DisplayName("포인트 잔액 조회 성공")
    void getBalance_success() throws Exception {
        // given
        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER)
                .balance(new BigDecimal("10000")).build();
        given(pointService.getBalance(1L)).willReturn(PointResponse.from(user));

        // when & then
        mockMvc.perform(get("/users/point").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.balance").value(10000))
                .andDo(restDocsHandler("point/balance"));
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void charge_success() throws Exception {
        // given
        String requestBody = """
                {
                    "amount": 5000
                }
                """;

        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER)
                .balance(new BigDecimal("15000")).build();
        given(pointService.charge(eq(1L), any())).willReturn(PointResponse.from(user));

        // when & then
        mockMvc.perform(patch("/users/point/charge")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(15000))
                .andDo(restDocsHandler("point/charge"));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 금액이 0 이하")
    void charge_fail_invalid_amount() throws Exception {
        // given
        String requestBody = """
                {
                    "amount": 0
                }
                """;

        // when & then
        mockMvc.perform(patch("/users/point/charge")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 내역 조회 성공")
    void getHistories_success() throws Exception {
        // given
        PointHistory history = PointHistory.builder()
                .user(User.builder().name("홍길동").email("test@test.com")
                        .password("encoded").userRole(UserRole.USER).build())
                .amount(new BigDecimal("5000"))
                .type(PointHistoryType.CHARGE)
                .build();
        ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.now());

        given(pointService.getHistories(1L)).willReturn(List.of(PointHistoryResponse.from(history)));

        // when & then
        mockMvc.perform(get("/users/point/histories").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].amount").value(5000))
                .andExpect(jsonPath("$.data[0].type").value("CHARGE"))
                .andDo(restDocsHandler("point/histories"));
    }
}