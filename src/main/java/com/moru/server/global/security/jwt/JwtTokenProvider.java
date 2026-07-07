package com.moru.server.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.moru.server.domain.member.entity.enums.Role;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long memberId, Role role) {
        return createToken(memberId, role, jwtProperties.getAccessTokenExpiration());
    }

    public String createRefreshToken(Long memberId, Role role) {
        return createToken(memberId, role, jwtProperties.getRefreshTokenExpiration());
    }

    private String createToken(Long memberId, Role role, Duration expiration) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
