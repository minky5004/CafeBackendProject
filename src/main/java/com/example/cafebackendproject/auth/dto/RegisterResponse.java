package com.example.cafebackendproject.auth.dto;

import com.example.cafebackendproject.domain.user.entity.User;
import lombok.Getter;

@Getter
public class RegisterResponse {

    private final Long userId;
    private final String name;
    private final String email;

    private RegisterResponse(Long userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getName(), user.getEmail());
    }
}