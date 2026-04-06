package com.example.cafebackendproject.domain.point.repository;

import com.example.cafebackendproject.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
