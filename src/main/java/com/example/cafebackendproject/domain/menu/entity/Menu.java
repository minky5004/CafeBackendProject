package com.example.cafebackendproject.domain.menu.entity;

import com.example.cafebackendproject.domain.menu.enums.MenuCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuCategory category;

    // false면 품절 처리
    @Column(nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    public void update(String name, String description, BigDecimal price, MenuCategory category, boolean isAvailable) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isAvailable = isAvailable;
    }
}