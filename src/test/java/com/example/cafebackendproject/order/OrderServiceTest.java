package com.example.cafebackendproject.order;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.enums.MenuCategory;
import com.example.cafebackendproject.domain.menu.repository.MenuRepository;
import com.example.cafebackendproject.domain.order.entity.Order;
import com.example.cafebackendproject.domain.order.enums.OrderStatus;
import com.example.cafebackendproject.domain.order.repository.OrderRepository;
import com.example.cafebackendproject.domain.point.entity.PointHistory;
import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import com.example.cafebackendproject.domain.point.repository.PointHistoryRepository;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import com.example.cafebackendproject.order.dto.OrderCreateRequest;
import com.example.cafebackendproject.order.dto.OrderItemRequest;
import com.example.cafebackendproject.order.dto.OrderResponse;
import com.example.cafebackendproject.order.dto.OrderStatusUpdateRequest;
import com.example.cafebackendproject.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "orderEventProducer", Optional.empty());
    }

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    private User sampleUser(BigDecimal balance) {
        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER)
                .balance(balance).build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Menu sampleMenu() {
        Menu menu = Menu.builder()
                .name("아메리카노").description("깔끔한 아메리카노")
                .price(new BigDecimal("4500")).category(MenuCategory.COFFEE)
                .build();
        ReflectionTestUtils.setField(menu, "id", 1L);
        return menu;
    }

    @Test
    @DisplayName("주문 생성 성공")
    void create_success() {
        // given
        User user = sampleUser(new BigDecimal("10000"));
        Menu menu = sampleMenu();

        OrderItemRequest itemRequest = new OrderItemRequest();
        ReflectionTestUtils.setField(itemRequest, "menuId", 1L);
        ReflectionTestUtils.setField(itemRequest, "quantity", 2);

        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "items", List.of(itemRequest));

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));

        Order savedOrder = Order.of(user, new BigDecimal("9000"));
        ReflectionTestUtils.setField(savedOrder, "id", 1L);
        ReflectionTestUtils.setField(savedOrder, "createdAt", LocalDateTime.now());
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderResponse response = orderService.create(1L, request);

        // then
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 메뉴")
    void create_fail_menu_not_found() {
        // given
        User user = sampleUser(new BigDecimal("10000"));

        OrderItemRequest itemRequest = new OrderItemRequest();
        ReflectionTestUtils.setField(itemRequest, "menuId", 99L);
        ReflectionTestUtils.setField(itemRequest, "quantity", 1);

        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "items", List.of(itemRequest));

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(menuRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.MENU_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("결제 성공")
    void payment_success() {
        // given
        User user = sampleUser(new BigDecimal("10000"));
        Order order = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());

        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pointHistoryRepository.save(any(PointHistory.class))).willReturn(null);

        // when
        OrderResponse response = orderService.payment(1L, 1L);

        // then
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(user.getBalance()).isEqualByComparingTo("5500");

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PointHistoryType.DEDUCT);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("4500");
    }

    @Test
    @DisplayName("결제 실패 - 이미 결제된 주문")
    void payment_fail_already_paid() {
        // given
        User user = sampleUser(new BigDecimal("10000"));
        Order order = Order.of(user, new BigDecimal("4500"));
        order.updateStatus(OrderStatus.PAID);
        ReflectionTestUtils.setField(order, "id", 1L);

        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.payment(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ORDER_ALREADY_PAID.getMessage());
    }

    @Test
    @DisplayName("결제 실패 - 포인트 잔액 부족")
    void payment_fail_insufficient_balance() {
        // given
        User user = sampleUser(new BigDecimal("1000")); // 잔액 부족
        Order order = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(order, "id", 1L);

        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> orderService.payment(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
    }

    @Test
    @DisplayName("주문 상태 변경 성공")
    void updateStatus_success() {
        // given
        User user = sampleUser(new BigDecimal("10000"));
        Order order = Order.of(user, new BigDecimal("4500"));
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());

        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", OrderStatus.PAID);

        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when
        OrderResponse response = orderService.updateStatus(1L, request);

        // then
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 상태 변경 실패 - 존재하지 않는 주문")
    void updateStatus_fail_not_found() {
        // given
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", OrderStatus.PAID);

        given(orderRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.updateStatus(99L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }
}