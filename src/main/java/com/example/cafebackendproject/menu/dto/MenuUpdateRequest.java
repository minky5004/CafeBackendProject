package com.example.cafebackendproject.menu.dto;

import com.example.cafebackendproject.domain.menu.enums.MenuCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MenuUpdateRequest {

    @NotBlank(message = "메뉴 이름을 입력해주세요.")
    private String name;

    private String description;

    @NotNull(message = "가격을 입력해주세요.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal price;

    @NotNull(message = "카테고리를 선택해주세요.")
    private MenuCategory category;

    @NotNull(message = "판매 여부를 입력해주세요.")
    private boolean isAvailable;
}