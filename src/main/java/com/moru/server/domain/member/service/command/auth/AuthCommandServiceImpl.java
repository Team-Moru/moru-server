package com.moru.server.domain.member.service.command.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.RefreshToken;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.repository.RefreshTokenRepository;
import com.moru.server.global.security.jwt.JwtTokenProvider;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandServiceImpl implements AuthCommandService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String DEFAULT_DEV_NICKNAME = "테스트회원";
    private static final LoginType DEFAULT_DEV_LOGIN_TYPE = LoginType.KAKAO;
    private static final String REFRESH_TOKEN_HASH_ALGORITHM = "SHA-256";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponseDTO.TokenResponse issueDevToken(AuthRequestDTO.DevTokenRequest request) {
        Member member = memberRepository.findByOauthId(request.oauthId())
                .orElseGet(() -> memberRepository.save(createDevMember(request)));
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getRole());

        saveRefreshToken(member, refreshToken);

        return AuthResponseDTO.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .memberId(member.getId())
                .onboardingCompleted(member.getOnboardingCompleted())
                .build();
    }

    private Member createDevMember(AuthRequestDTO.DevTokenRequest request) {
        return Member.builder()
                .oauthId(request.oauthId())
                .nickname(resolveNickname(request.nickname()))
                .role(Role.MEMBER)
                .loginType(DEFAULT_DEV_LOGIN_TYPE)
                .build();
    }

    private void saveRefreshToken(Member member, String refreshToken) {
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.findAllByMember_IdAndRevokedAtIsNull(member.getId())
                .forEach(token -> token.revoke(now));

        refreshTokenRepository.save(RefreshToken.builder()
                .member(member)
                .tokenHash(hashRefreshToken(refreshToken))
                .expiresAt(jwtTokenProvider.getRefreshTokenExpiresAt(refreshToken))
                .build());
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(REFRESH_TOKEN_HASH_ALGORITHM);
            byte[] hashedToken = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedToken);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("지원하지 않는 토큰 해시 알고리즘입니다.", e);
        }
    }

    private String resolveNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_DEV_NICKNAME;
        }
        return nickname;
    }

}
