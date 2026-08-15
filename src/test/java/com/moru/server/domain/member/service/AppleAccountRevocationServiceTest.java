package com.moru.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.member.client.AppleOAuthTokenClient;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.global.config.AppleWithdrawalProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@ExtendWith(MockitoExtension.class)
class AppleAccountRevocationServiceTest {

    @Mock
    private AppleOAuthCredentialService credentialService;

    @Mock
    private AppleOAuthTokenClient appleOAuthTokenClient;

    @Mock
    private AppleWithdrawalProperties withdrawalProperties;

    @InjectMocks
    private AppleAccountRevocationService revocationService;

    @Test
    void revokesStoredRefreshTokenForAppleMember() {
        when(credentialService.findRefreshToken(1L))
                .thenReturn(Optional.of("apple-refresh-token"));

        revocationService.revokeIfRequired(1L, LoginType.APPLE);

        verify(appleOAuthTokenClient).revokeRefreshToken("apple-refresh-token");
    }

    @Test
    void requiresReauthenticationWhenAppleCredentialIsMissingAndFallbackIsDisabled() {
        when(credentialService.findRefreshToken(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revocationService.revokeIfRequired(1L, LoginType.APPLE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.APPLE_REAUTH_REQUIRED));
    }

    @Test
    void skipsRevokeForLegacyAppleMemberWhenMigrationFallbackIsEnabled() {
        when(credentialService.findRefreshToken(1L)).thenReturn(Optional.empty());
        when(withdrawalProperties.isAllowMissingCredential()).thenReturn(true);

        revocationService.revokeIfRequired(1L, LoginType.APPLE);

        verifyNoInteractions(appleOAuthTokenClient);
    }

    @Test
    void skipsRevokeForNonAppleMember() {
        revocationService.revokeIfRequired(1L, LoginType.GOOGLE);

        verify(credentialService, never()).findRefreshToken(1L);
        verifyNoInteractions(appleOAuthTokenClient);
    }
}
