package com.moru.server.domain.member.service;

import org.springframework.stereotype.Service;

import com.moru.server.domain.member.client.AppleOAuthTokenClient;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.global.config.AppleWithdrawalProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppleAccountRevocationService {

    private final AppleOAuthCredentialService credentialService;
    private final AppleOAuthTokenClient appleOAuthTokenClient;
    private final AppleWithdrawalProperties withdrawalProperties;

    public void revokeIfRequired(Long memberId, LoginType loginType) {
        if (loginType != LoginType.APPLE) {
            return;
        }

        String refreshToken = credentialService.findRefreshToken(memberId).orElse(null);
        if (refreshToken == null) {
            if (withdrawalProperties.isAllowMissingCredential()) {
                log.warn("Apple credential 마이그레이션 fallback으로 revoke를 건너뜁니다.");
                return;
            }
            throw new BusinessException(ErrorStatus.APPLE_REAUTH_REQUIRED);
        }
        appleOAuthTokenClient.revokeRefreshToken(refreshToken);
    }
}
