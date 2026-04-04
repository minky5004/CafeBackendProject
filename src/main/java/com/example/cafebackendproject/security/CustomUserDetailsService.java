package com.example.cafebackendproject.security;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.domain.user.entity.User;
import com.example.cafebackendproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security가 인증 시 이메일로 사용자 로드
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new CustomUserDetails(user.getId(), user.getEmail(), user.getUserRole());
    }
}