package com.example.cafebackendproject.menu.dto;

import com.example.cafebackendproject.domain.menu.entity.Menu;
import com.example.cafebackendproject.domain.menu.enums.MenuCategory;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MenuResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final MenuCategory category;
    private final boolean isAvailable;

    private MenuResponse(Long id, String name, String description,
                         BigDecimal price, MenuCategory category, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isAvailable = isAvailable;
    }

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getPrice(),
                menu.getCategory(),
                menu.isAvailable()
        );
    }
}