package com.example.cafebackendproject.order;

import com.example.cafebackendproject.RestDocsSupport;
import com.example.cafebackendproject.config.TestRedissonConfig;
import com.example.cafebackendproject.domain.order.entity.Order;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.order.controller.UserOrderController;
import com.example.cafebackendproject.order.dto.OrderResponse;
import com.example.cafebackendproject.order.service.OrderService;
import com.example.cafebackendproject.security.CustomUserDetails;
import com.example.cafebackendproject.security.SecurityConfig;
import com.example.cafebackendproject.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserOrderController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import({SecurityConfig.class, TestRedissonConfig.class})
@ActiveProfiles("test")
class UserOrderControllerTest extends RestDocsSupport {

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final CustomUserDetails userDetails =
            new CustomUserDetails(1L, "test@test.com", UserRole.USER);

    @Test
    @DisplayName("내 주문 내역 조회 성공")
    void getMyOrders_success() throws Exception {
        // given
        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER).build();
        Order order = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());

        given(orderService.getMyOrders(1L)).willReturn(List.of(OrderResponse.from(order)));

        // when & then
        mockMvc.perform(get("/users/orders").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value(1L))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andDo(restDocsHandler("order/my-orders"));
    }

    @Test
    @DisplayName("내 주문 내역 조회 실패 - 미인증")
    void getMyOrders_fail_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/users/orders"))
                .andExpect(status().isUnauthorized());
    }
}