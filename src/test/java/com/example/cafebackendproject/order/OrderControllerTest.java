package com.example.cafebackendproject.order;

import com.example.cafebackendproject.RestDocsSupport;
import com.example.cafebackendproject.config.TestRedissonConfig;
import com.example.cafebackendproject.domain.order.entity.Order;
import com.example.cafebackendproject.domain.order.enums.OrderStatus;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.order.controller.OrderController;
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

@WebMvcTest(OrderController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import({SecurityConfig.class, TestRedissonConfig.class})
@ActiveProfiles("test")
class OrderControllerTest extends RestDocsSupport {

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final CustomUserDetails userDetails =
            new CustomUserDetails(1L, "test@test.com", UserRole.USER);

    private final CustomUserDetails adminDetails =
            new CustomUserDetails(1L, "admin@test.com", UserRole.ADMIN);

    private OrderResponse sampleOrderResponse() {
        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER).build();
        Order order = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
        return OrderResponse.from(order);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_success() throws Exception {
        // given
        String requestBody = """
                {
                    "items": [
                        { "menuId": 1, "quantity": 2 }
                    ]
                }
                """;

        given(orderService.create(eq(1L), any())).willReturn(sampleOrderResponse());

        // when & then
        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1L))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(restDocsHandler("order/create"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 주문 항목 없음")
    void createOrder_fail_empty_items() throws Exception {
        // given
        String requestBody = """
                {
                    "items": []
                }
                """;

        // when & then
        mockMvc.perform(post("/orders")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전체 주문 조회 성공 - 관리자")
    void getAllOrders_success() throws Exception {
        // given
        given(orderService.getAllOrders()).willReturn(List.of(sampleOrderResponse()));

        // when & then
        mockMvc.perform(get("/orders").with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(1L))
                .andDo(restDocsHandler("order/list"));
    }

    @Test
    @DisplayName("전체 주문 조회 실패 - 권한 없음 (일반 사용자)")
    void getAllOrders_fail_forbidden() throws Exception {
        // when & then
        mockMvc.perform(get("/orders").with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("주문 상태 변경 성공 - 관리자")
    void updateOrderStatus_success() throws Exception {
        // given
        String requestBody = """
                {
                    "status": "PAID"
                }
                """;

        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER).build();
        Order paidOrder = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(paidOrder, "id", 1L);
        ReflectionTestUtils.setField(paidOrder, "createdAt", LocalDateTime.now());
        paidOrder.updateStatus(OrderStatus.PAID);

        given(orderService.updateStatus(eq(1L), any())).willReturn(OrderResponse.from(paidOrder));

        // when & then
        mockMvc.perform(patch("/orders/1/status")
                        .with(user(adminDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andDo(restDocsHandler("order/status"));
    }

    @Test
    @DisplayName("결제 성공")
    void payment_success() throws Exception {
        // given
        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER).build();
        Order paidOrder = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(paidOrder, "id", 1L);
        ReflectionTestUtils.setField(paidOrder, "createdAt", LocalDateTime.now());
        paidOrder.updateStatus(OrderStatus.PAID);

        given(orderService.payment(eq(1L), eq(1L))).willReturn(OrderResponse.from(paidOrder));

        // when & then
        mockMvc.perform(post("/orders/payment")
                        .with(user(userDetails))
                        .param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andDo(restDocsHandler("order/payment"));
    }
}