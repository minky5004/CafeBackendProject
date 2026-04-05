package com.example.cafebackendproject.auth;

import com.example.cafebackendproject.RestDocsSupport;
import com.example.cafebackendproject.auth.controller.AuthController;
import com.example.cafebackendproject.auth.dto.LoginResponse;
import com.example.cafebackendproject.auth.dto.RegisterResponse;
import com.example.cafebackendproject.auth.service.AuthService;
import com.example.cafebackendproject.config.TestRedissonConfig;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.enums.UserRole;
import com.example.cafebackendproject.security.SecurityConfig;
import com.example.cafebackendproject.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import({SecurityConfig.class, TestRedissonConfig.class})
@ActiveProfiles("test")
class AuthControllerTest extends RestDocsSupport {

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "홍길동",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;

        User user = User.builder()
                .name("홍길동").email("test@test.com")
                .password("encoded").userRole(UserRole.USER).build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);

        given(authService.register(any())).willReturn(RegisterResponse.from(user));

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andDo(restDocsHandler("auth/signup"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 오류")
    void register_fail_invalid_email() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "홍길동",
                    "email": "invalid-email",
                    "password": "password123"
                }
                """;

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 길이 미달")
    void register_fail_short_password() throws Exception {
        // given
        String requestBody = """
                {
                    "name": "홍길동",
                    "email": "test@test.com",
                    "password": "short"
                }
                """;

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;

        given(authService.login(any())).willReturn(new LoginResponse("test-jwt-token"));

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer test-jwt-token"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("test-jwt-token"))
                .andDo(restDocsHandler("auth/login"));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 형식 오류")
    void login_fail_invalid_email() throws Exception {
        // given
        String requestBody = """
                {
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andDo(restDocsHandler("auth/logout"));
    }
}