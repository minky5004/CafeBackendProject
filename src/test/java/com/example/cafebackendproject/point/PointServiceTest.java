package com.example.cafebackendproject.point;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.point.entity.PointHistory;
import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import com.example.cafebackendproject.domain.point.repository.PointHistoryRepository;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import com.example.cafebackendproject.point.dto.PointChargeRequest;
import com.example.cafebackendproject.point.dto.PointHistoryResponse;
import com.example.cafebackendproject.point.dto.PointResponse;
import com.example.cafebackendproject.point.service.PointService;
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
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

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

    @Test
    @DisplayName("포인트 잔액 조회 성공")
    void getBalance_success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(sampleUser(new BigDecimal("10000"))));

        // when
        PointResponse response = pointService.getBalance(1L);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("포인트 잔액 조회 실패 - 사용자 없음")
    void getBalance_fail_user_not_found() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.getBalance(99L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void charge_success() {
        // given
        User user = sampleUser(new BigDecimal("10000"));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pointHistoryRepository.save(any(PointHistory.class))).willReturn(null);

        PointChargeRequest request = new PointChargeRequest();
        ReflectionTestUtils.setField(request, "amount", new BigDecimal("5000"));

        // when
        PointResponse response = pointService.charge(1L, request);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo("15000");

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PointHistoryType.CHARGE);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("포인트 충전 실패 - 사용자 없음")
    void charge_fail_user_not_found() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        PointChargeRequest request = new PointChargeRequest();
        ReflectionTestUtils.setField(request, "amount", new BigDecimal("5000"));

        // when & then
        assertThatThrownBy(() -> pointService.charge(99L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("포인트 내역 조회 성공")
    void getHistories_success() {
        // given
        given(userRepository.existsById(1L)).willReturn(true);

        User user = sampleUser(new BigDecimal("10000"));
        PointHistory history = PointHistory.builder()
                .user(user).amount(new BigDecimal("5000")).type(PointHistoryType.CHARGE).build();
        ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.now());

        given(pointHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(history));

        // when
        List<PointHistoryResponse> responses = pointService.getHistories(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getType()).isEqualTo(PointHistoryType.CHARGE);
        assertThat(responses.get(0).getAmount()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("포인트 내역 조회 실패 - 사용자 없음")
    void getHistories_fail_user_not_found() {
        // given
        given(userRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> pointService.getHistories(99L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }
}