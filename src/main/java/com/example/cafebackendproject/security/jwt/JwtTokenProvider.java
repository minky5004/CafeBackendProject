package com.example.cafebackendproject.security.jwt;

import com.example.cafebackendproject.common.exception.CustomException;
import com.example.cafebackendproject.common.exception.ErrorCode;
import com.example.cafebackendproject.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // 액세스 토큰 생성 (subject: email, claim: userId, role)
    public String createToken(CustomUserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userDetails.getEmail())
                .claim("userId", userDetails.getUserId())
                .claim("role", userDetails.getUserRole().name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 Claims 추출
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
}