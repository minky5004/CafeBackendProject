package com.example.cafebackendproject.auth.service;

import com.example.cafebackendproject.auth.dto.LoginRequest;
import com.example.cafebackendproject.auth.dto.LoginResponse;
import com.example.cafebackendproject.auth.dto.RegisterRequest;
import com.example.cafebackendproject.auth.dto.RegisterResponse;
import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import com.example.cafebackendproject.security.CustomUserDetails;
import com.example.cafebackendproject.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userRole(UserRole.USER)
                .build();

        return RegisterResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getEmail(), user.getUserRole());
        return new LoginResponse(jwtTokenProvider.createToken(userDetails));
    }
}