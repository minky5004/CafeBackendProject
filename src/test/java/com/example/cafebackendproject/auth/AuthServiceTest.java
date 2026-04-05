package com.example.cafebackendproject.auth;

import com.example.cafebackendproject.auth.dto.LoginRequest;
import com.example.cafebackendproject.auth.dto.LoginResponse;
import com.example.cafebackendproject.auth.dto.RegisterRequest;
import com.example.cafebackendproject.auth.dto.RegisterResponse;
import com.example.cafebackendproject.auth.service.AuthService;
import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import com.example.cafebackendproject.security.CustomUserDetails;
import com.example.cafebackendproject.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        ReflectionTestUtils.setField(registerRequest, "name", "홍길동");
        ReflectionTestUtils.setField(registerRequest, "email", "test@test.com");
        ReflectionTestUtils.setField(registerRequest, "password", "password123");
    }

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // given
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");

        User savedUser = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encodedPassword").userRole(UserRole.USER).build();
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        RegisterResponse response = authService.register(registerRequest);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User captured = captor.getValue();
        assertThat(captured.getUserRole()).isEqualTo(UserRole.USER);
        assertThat(captured.getPassword()).isEqualTo("encodedPassword");

        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_fail_duplicate_email() {
        // given
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage());

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        ReflectionTestUtils.setField(loginRequest, "email", "test@test.com");
        ReflectionTestUtils.setField(loginRequest, "password", "password123");

        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encodedPassword").userRole(UserRole.USER).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.createToken(any(CustomUserDetails.class))).willReturn("jwt-token");

        // when
        LoginResponse response = authService.login(loginRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        verify(jwtTokenProvider).createToken(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_fail_user_not_found() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        ReflectionTestUtils.setField(loginRequest, "email", "notfound@test.com");
        ReflectionTestUtils.setField(loginRequest, "password", "password123");

        given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtTokenProvider, never()).createToken(any());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_invalid_password() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        ReflectionTestUtils.setField(loginRequest, "email", "test@test.com");
        ReflectionTestUtils.setField(loginRequest, "password", "wrongPassword");

        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encodedPassword").userRole(UserRole.USER).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_PASSWORD.getMessage());

        verify(jwtTokenProvider, never()).createToken(any());
    }
}