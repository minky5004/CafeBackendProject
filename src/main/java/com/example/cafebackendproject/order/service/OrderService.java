package com.example.cafebackendproject.order.service;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.repository.MenuRepository;
import com.example.cafebackendproject.domain.order.entity.Order;
import com.example.cafebackendproject.domain.order.entity.OrderItem;
import com.example.cafebackendproject.domain.order.enums.OrderStatus;
import com.example.cafebackendproject.domain.order.repository.OrderRepository;
import com.example.cafebackendproject.domain.point.entity.PointHistory;
import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import com.example.cafebackendproject.domain.point.repository.PointHistoryRepository;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import com.example.cafebackendproject.order.dto.OrderCreateRequest;
import com.example.cafebackendproject.order.dto.OrderItemRequest;
import com.example.cafebackendproject.order.dto.OrderResponse;
import com.example.cafebackendproject.order.dto.OrderStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public OrderResponse create(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 메뉴 조회 및 검증을 먼저 수행 후 총액 계산
        List<Menu> menus = request.getItems().stream()
                .map(item -> {
                    Menu menu = menuRepository.findById(item.getMenuId())
                            .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
                    if (!menu.isAvailable() || menu.isDeleted()) {
                        throw new CustomException(ErrorCode.MENU_UNAVAILABLE);
                    }
                    return menu;
                })
                .toList();

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int i = 0; i < menus.size(); i++) {
            totalPrice = totalPrice.add(
                    menus.get(i).getPrice().multiply(BigDecimal.valueOf(request.getItems().get(i).getQuantity()))
            );
        }

        // Order 생성 후 OrderItem cascade 저장
        Order order = Order.of(user, totalPrice);
        for (int i = 0; i < menus.size(); i++) {
            OrderItemRequest itemRequest = request.getItems().get(i);
            order.getOrderItems().add(OrderItem.of(order, menus.get(i), itemRequest.getQuantity()));
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    // 전체 주문 조회 (관리자용)
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    // 내 주문 내역 조회
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    // 주문 상태 변경 (관리자용)
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.updateStatus(request.getStatus());
        return OrderResponse.from(order);
    }

    // 결제: 포인트 차감 → 주문 상태 PAID로 변경
    @Transactional
    public OrderResponse payment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getBalance().compareTo(order.getTotalPrice()) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        user.deduct(order.getTotalPrice());
        pointHistoryRepository.save(PointHistory.of(user, order.getTotalPrice(), PointHistoryType.DEDUCT));
        order.updateStatus(OrderStatus.PAID);

        return OrderResponse.from(order);
    }
}