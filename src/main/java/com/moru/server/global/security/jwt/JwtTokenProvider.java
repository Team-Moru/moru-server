package com.moru.server.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

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

    public Long getMemberId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public Role getRole(String token) {
        String role = parseClaims(token).get(ROLE_CLAIM, String.class);
        return Role.valueOf(role);
    }

    public void validateAccessToken(String token) {
        parseClaims(token);
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

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorStatus.ACCESS_TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw new BusinessException(ErrorStatus.MALFORMED_TOKEN);
        } catch (io.jsonwebtoken.security.SecurityException e) {
            throw new BusinessException(ErrorStatus.INVALID_SIGNATURE);
        } catch (UnsupportedJwtException e) {
            throw new BusinessException(ErrorStatus.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorStatus.ILLEGAL_ARGUMENT_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(ErrorStatus.TOKEN_PARSING_ERROR);
        }
    }
}
