package com.example.cafebackendproject.point.service;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.point.entity.PointHistory;
import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import com.example.cafebackendproject.domain.point.repository.PointHistoryRepository;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import com.example.cafebackendproject.point.dto.PointChargeRequest;
import com.example.cafebackendproject.point.dto.PointHistoryResponse;
import com.example.cafebackendproject.point.dto.PointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public PointResponse getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return PointResponse.from(user);
    }

    @Transactional
    public PointResponse charge(Long userId, PointChargeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.charge(request.getAmount());
        pointHistoryRepository.save(PointHistory.of(user, request.getAmount(), PointHistoryType.CHARGE));
        return PointResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getHistories(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return pointHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PointHistoryResponse::from)
                .toList();
    }
}