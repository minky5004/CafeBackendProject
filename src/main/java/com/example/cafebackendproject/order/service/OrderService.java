package com.example.cafebackendproject.order.service;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.common.lock.DistributedLock;
import com.example.cafebackendproject.order.event.OrderEventPayload;
import com.example.cafebackendproject.order.event.OrderEventProducer;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final Optional<OrderEventProducer> orderEventProducer;

    @Transactional
    public OrderResponse create(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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

        Order order = Order.of(user, totalPrice);
        for (int i = 0; i < menus.size(); i++) {
            OrderItemRequest itemRequest = request.getItems().get(i);
            order.getOrderItems().add(OrderItem.of(order, menus.get(i), itemRequest.getQuantity()));
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.updateStatus(request.getStatus());
        return OrderResponse.from(order);
    }

    @DistributedLock(key = "#userId")
    @Transactional
    public OrderResponse payment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_FORBIDDEN);
        }

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

        orderEventProducer.ifPresent(producer -> producer.sendOrderPaidEvent(OrderEventPayload.from(order)));

        return OrderResponse.from(order);
    }
}
