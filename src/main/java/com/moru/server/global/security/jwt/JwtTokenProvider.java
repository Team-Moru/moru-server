package com.moru.server.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long memberId, Role role) {
        return createToken(memberId, role, jwtProperties.getAccessTokenExpiration(), ACCESS_TOKEN_TYPE);
    }

    public String createRefreshToken(Long memberId, Role role) {
        return createToken(memberId, role, jwtProperties.getRefreshTokenExpiration(), REFRESH_TOKEN_TYPE);
    }

    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);

        return extractMemberId(claims);
    }

    public Long getMemberIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token, ErrorStatus.REFRESH_TOKEN_EXPIRED);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);

        return extractMemberId(claims);
    }

    public LocalDateTime getRefreshTokenExpiresAt(String token) {
        Claims claims = parseClaims(token, ErrorStatus.REFRESH_TOKEN_EXPIRED);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);

        Date expiration = claims.getExpiration();

        if (expiration == null) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    private Long extractMemberId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorStatus.ILLEGAL_ARGUMENT_TOKEN);
        }
    }

    public Role getRole(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);

        String role = claims.get(ROLE_CLAIM, String.class);

        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorStatus.ILLEGAL_ARGUMENT_TOKEN);
        }
    }

    public void validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);
    }

    public void validateRefreshToken(String token) {
        Claims claims = parseClaims(token, ErrorStatus.REFRESH_TOKEN_EXPIRED);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);
    }

    private String createToken(Long memberId, Role role, Duration expiration, String tokenType) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseClaims(String token) {
        return parseClaims(token, ErrorStatus.ACCESS_TOKEN_EXPIRED);
    }

    private Claims parseClaims(String token, ErrorStatus expiredErrorStatus) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(expiredErrorStatus);
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

    private void validateTokenType(Claims claims, String expectedTokenType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedTokenType.equals(tokenType)) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }
}
