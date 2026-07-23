package com.moru.server.domain.member.entity.enums;

import java.util.Locale;

import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

public enum OAuthProvider {
    KAKAO(LoginType.KAKAO),
    GOOGLE(LoginType.GOOGLE),
    APPLE(LoginType.APPLE);

    private final LoginType loginType;

    OAuthProvider(LoginType loginType) {
        this.loginType = loginType;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public static OAuthProvider from(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorStatus.INVALID_SOCIAL_PROVIDER);
        }
    }
}
