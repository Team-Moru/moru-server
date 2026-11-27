package com.moru.server.domain.member.service.command.auth;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.redis.UsageQuotaService;
import com.moru.server.global.redis.UsageQuotaService.Limits;
import com.moru.server.global.response.code.status.ErrorStatus;
import com.moru.server.global.security.jwt.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AnonymousAuthService {
    private final JdbcTemplate jdbc;
    private final JwtTokenProvider jwt;
    private final UsageQuotaService quota;
    private final V2UsageProperties properties;

    public Tokens register(UUID installationId, String ipHash) {
        quota.consume("anonymous-register", ipHash, new Limits(properties.getRegistrationPerMinute(), null),
                null, ErrorStatus.ANONYMOUS_AUTH_RATE_LIMITED);
        UUID id = UUID.randomUUID();
        Tokens tokens = createTokens(id);
        try {
            jdbc.update("""
                    INSERT INTO anonymous_installation (installation_id, anonymous_id, refresh_hash)
                    VALUES (?, ?, ?)
                    """, installationId.toString(), id.toString(), DigestUtils.sha256Hex(tokens.refreshToken()));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorStatus.INSTALLATION_ALREADY_REGISTERED);
        }
        return tokens;
    }

    public Tokens refresh(String refreshToken) {
        UUID id = jwt.getInstallationIdFromRefreshToken(refreshToken);
        quota.consume("anonymous-refresh", id.toString(), new Limits(properties.getRefreshPerMinute(), null),
                null, ErrorStatus.ANONYMOUS_AUTH_RATE_LIMITED);
        Tokens next = createTokens(id);
        int updated = jdbc.update("""
                UPDATE anonymous_installation SET refresh_hash = ?
                WHERE anonymous_id = ? AND refresh_hash = ?
                """, DigestUtils.sha256Hex(next.refreshToken()), id.toString(), DigestUtils.sha256Hex(refreshToken));
        if (updated != 1) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
        return next;
    }

    private Tokens createTokens(UUID id) {
        return new Tokens(jwt.createInstallationAccessToken(id), jwt.createInstallationRefreshToken(id));
    }

    public record Tokens(String accessToken, String refreshToken) {
    }
}
