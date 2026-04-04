package com.example.cafebackendproject.domain.point.repository;

import com.example.cafebackendproject.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 특정 사용자의 포인트 내역 (최신순)
    List<PointHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}