package com.moru.server.domain.member.service.command.auth;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.redis.UsageQuotaService;
import com.moru.server.global.response.code.status.ErrorStatus;
import com.moru.server.global.security.auth.AuthenticatedInstallation;
import com.moru.server.global.security.jwt.JwtProperties;
import com.moru.server.global.security.jwt.JwtTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AnonymousAuthServiceTest {
    @Test
    void rotatesRefreshCredentialsAndPreservesInstallationIdentity() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE anonymous_installation (
                    installation_id CHAR(36) PRIMARY KEY,
                    anonymous_id CHAR(36) UNIQUE NOT NULL,
                    refresh_hash CHAR(64) NOT NULL
                )
                """);
        var properties = new JwtProperties();
        properties.setSecret("anonymous-auth-test-signing-key-with-sufficient-length");
        properties.setAccessTokenExpiration(Duration.ofHours(1));
        properties.setRefreshTokenExpiration(Duration.ofDays(14));
        var jwt = new JwtTokenProvider(properties);
        var service = new AnonymousAuthService(jdbc, jwt, mock(UsageQuotaService.class), new V2UsageProperties());

        UUID installationId = UUID.randomUUID();
        var first = service.register(installationId, "test-ip");
        var next = service.refresh(first.refreshToken());

        assertThat(jwt.getAccessPrincipal(first.accessToken())).isInstanceOf(AuthenticatedInstallation.class)
                .isEqualTo(jwt.getAccessPrincipal(next.accessToken()));
        assertThat(next.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getBaseCode()).isEqualTo(ErrorStatus.INVALID_TOKEN));
        assertThatThrownBy(() -> service.register(installationId, "test-ip"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getBaseCode()).isEqualTo(ErrorStatus.INSTALLATION_ALREADY_REGISTERED));
        assertThatThrownBy(() -> jwt.getAccessPrincipal(next.refreshToken())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> jwt.getMemberId(next.accessToken())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.refresh(jwt.createRefreshToken(1L, Role.MEMBER)))
                .isInstanceOf(BusinessException.class);
    }
}
