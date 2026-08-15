package com.moru.server.domain.member.service;

import org.springframework.stereotype.Service;

import com.moru.server.domain.member.client.AppleOAuthTokenClient;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppleAccountRevocationService {

    private final AppleOAuthCredentialService credentialService;
    private final AppleOAuthTokenClient appleOAuthTokenClient;

    public void revokeIfRequired(Long memberId, LoginType loginType) {
        if (loginType != LoginType.APPLE) {
            return;
        }

        String refreshToken = credentialService.findRefreshToken(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.APPLE_REAUTH_REQUIRED));
        appleOAuthTokenClient.revokeRefreshToken(refreshToken);
    }
}
