package com.moru.server.domain.member.service.command.auth;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.enums.OAuthProvider;

public interface AuthCommandService {

    AuthResponseDTO.TokenResponse issueDevToken(AuthRequestDTO.DevTokenRequest request);

    AuthResponseDTO.SocialLoginResponse loginWithSocial(
            OAuthProvider provider,
            AuthRequestDTO.SocialLoginRequest request
    );

    AuthResponseDTO.TokenResponse reissueToken(String refreshToken);

    void logout(Long memberId, AuthRequestDTO.LogoutRequest request);

    AuthResponseDTO.WithdrawalResponse withdraw(Long memberId);
}
