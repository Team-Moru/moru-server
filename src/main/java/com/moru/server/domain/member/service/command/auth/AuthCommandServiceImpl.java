package com.moru.server.domain.member.service.command.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.RefreshToken;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.repository.RefreshTokenRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
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

        return issueTokenResponse(member);
    }

    @Override
    public AuthResponseDTO.TokenResponse reissueToken(String refreshToken) {
        validateRefreshTokenRequired(refreshToken);
        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));
        RefreshToken storedRefreshToken = findStoredRefreshToken(memberId, refreshToken);

        validateStoredRefreshToken(storedRefreshToken);

        return issueTokenResponse(member);
    }

    @Override
    public void logout(Long memberId, AuthRequestDTO.LogoutRequest request) {
        String refreshToken = request.refreshToken();

        validateRefreshTokenRequired(refreshToken);
        jwtTokenProvider.validateRefreshToken(refreshToken);
        validateRefreshTokenOwner(memberId, refreshToken);

        RefreshToken storedRefreshToken = findStoredRefreshToken(memberId, refreshToken);

        validateStoredRefreshToken(storedRefreshToken);
        storedRefreshToken.revoke(LocalDateTime.now());
    }

    private AuthResponseDTO.TokenResponse issueTokenResponse(Member member) {
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

    private RefreshToken findStoredRefreshToken(Long memberId, String refreshToken) {
        return refreshTokenRepository.findByMember_IdAndTokenHash(memberId, hashRefreshToken(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND));
    }

    private void validateStoredRefreshToken(RefreshToken refreshToken) {
        LocalDateTime now = LocalDateTime.now();

        if (refreshToken.isRevoked()) {
            throw new BusinessException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND);
        }

        if (refreshToken.isExpired(now)) {
            refreshToken.revoke(now);
            throw new BusinessException(ErrorStatus.REFRESH_TOKEN_EXPIRED);
        }
    }

    private void validateRefreshTokenOwner(Long memberId, String refreshToken) {
        Long refreshTokenMemberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);

        if (!memberId.equals(refreshTokenMemberId)) {
            throw new BusinessException(ErrorStatus.REFRESH_TOKEN_MISMATCH);
        }
    }

    private void validateRefreshTokenRequired(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorStatus.TOKEN_MISSING);
        }
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
