package com.example.cafebackendproject.domain.point.entity;

import com.example.cafebackendproject.domain.point.enums.PointHistoryType;
import com.example.cafebackendproject.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    // 충전(CHARGE) 또는 차감(DEDUCT)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PointHistory of(User user, BigDecimal amount, PointHistoryType type) {
        return PointHistory.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .build();
    }
}