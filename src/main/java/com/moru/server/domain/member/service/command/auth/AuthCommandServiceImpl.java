package com.moru.server.domain.member.service.command.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.moru.server.domain.member.client.AppleOAuthClient;
import com.moru.server.domain.member.client.GoogleOAuthClient;
import com.moru.server.domain.member.client.KakaoOAuthClient;
import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.OAuthProvider;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.repository.MemberTermRepository;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.domain.subscriptions.repository.SubscriptionsRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import com.moru.server.global.security.jwt.JwtTokenProvider;
import com.moru.server.global.security.jwt.RefreshTokenStore;

@Service
@RequiredArgsConstructor
public class AuthCommandServiceImpl implements AuthCommandService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String DEFAULT_DEV_NICKNAME = "테스트회원";
    private static final LoginType DEFAULT_DEV_LOGIN_TYPE = LoginType.KAKAO;
    private static final String WITHDRAWAL_COMPLETE_MESSAGE = "회원 탈퇴가 완료되었습니다.";
    private static final String REFRESH_TOKEN_HASH_ALGORITHM = "SHA-256";

    private final MemberRepository memberRepository;
    private final MemberTermRepository memberTermRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final RoutineGroupRepository routineGroupRepository;
    private final SubscriptionsRepository subscriptionsRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final AppleOAuthClient appleOAuthClient;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponseDTO.TokenResponse issueDevToken(AuthRequestDTO.DevTokenRequest request) {
        Member member = findOrCreateMember(
                DEFAULT_DEV_LOGIN_TYPE,
                request.oauthId(),
                () -> createDevMember(request)
        ).member();

        return issueTokenResponse(member);
    }

    @Override
    public AuthResponseDTO.SocialLoginResponse loginWithSocial(
            OAuthProvider provider,
            AuthRequestDTO.SocialLoginRequest request
    ) {
        return switch (provider) {
            case KAKAO -> loginWithKakao(request);
            case GOOGLE -> loginWithGoogle(request);
            case APPLE -> loginWithApple(request);
        };
    }

    @Override
    @Transactional
    public AuthResponseDTO.TokenResponse reissueToken(String refreshToken) {
        validateRefreshTokenRequired(refreshToken);
        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String nextRefreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getRole());

        if (!refreshTokenStore.rotate(
                memberId,
                hashRefreshToken(refreshToken),
                hashRefreshToken(nextRefreshToken),
                getRefreshTokenTtl(nextRefreshToken)
        )) {
            throw new BusinessException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND);
        }

        return createTokenResponse(member, accessToken, nextRefreshToken);
    }

    @Override
    @Transactional
    public void logout(Long memberId, AuthRequestDTO.LogoutRequest request) {
        String refreshToken = request.refreshToken();

        validateRefreshTokenRequired(refreshToken);
        jwtTokenProvider.validateRefreshToken(refreshToken);
        validateRefreshTokenOwner(memberId, refreshToken);

        if (!refreshTokenStore.deleteIfMatches(memberId, hashRefreshToken(refreshToken))) {
            throw new BusinessException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND);
        }
    }

    private AuthResponseDTO.SocialLoginResponse loginWithKakao(AuthRequestDTO.SocialLoginRequest request) {
        KakaoOAuthClient.KakaoMemberInfo kakaoMemberInfo = kakaoOAuthClient.getMemberInfo(request.token());

        return loginOrCreateMember(
                kakaoMemberInfo.oauthId(),
                kakaoMemberInfo.nickname(),
                LoginType.KAKAO
        );
    }

    private AuthResponseDTO.SocialLoginResponse loginWithGoogle(AuthRequestDTO.SocialLoginRequest request) {
        GoogleOAuthClient.GoogleMemberInfo googleMemberInfo = googleOAuthClient.getMemberInfo(request.token());

        return loginOrCreateMember(
                googleMemberInfo.oauthId(),
                googleMemberInfo.nickname(),
                LoginType.GOOGLE
        );
    }

    private AuthResponseDTO.SocialLoginResponse loginWithApple(AuthRequestDTO.SocialLoginRequest request) {
        AppleOAuthClient.AppleMemberInfo appleMemberInfo = appleOAuthClient.getMemberInfo(request.token());

        return loginOrCreateMember(
                appleMemberInfo.oauthId(),
                appleMemberInfo.nickname(),
                LoginType.APPLE
        );
    }

    private AuthResponseDTO.SocialLoginResponse loginOrCreateMember(
            String oauthId,
            String nickname,
            LoginType loginType
    ) {
        MemberCreationResult result = findOrCreateMember(
                loginType,
                oauthId,
                () -> createSocialMember(oauthId, nickname, loginType)
        );

        return createSocialLoginResponse(result.member(), result.isNewMember());
    }

    private MemberCreationResult findOrCreateMember(
            LoginType loginType,
            String oauthId,
            Supplier<Member> memberSupplier
    ) {
        Optional<Member> existingMember = memberRepository.findByLoginTypeAndOauthId(loginType, oauthId);

        if (existingMember.isPresent()) {
            return new MemberCreationResult(existingMember.get(), false);
        }

        try {
            Member savedMember = memberRepository.saveAndFlush(memberSupplier.get());
            return new MemberCreationResult(savedMember, true);
        } catch (DataIntegrityViolationException exception) {
            Member concurrentlyCreatedMember = memberRepository.findByLoginTypeAndOauthId(loginType, oauthId)
                    .orElseThrow(() -> exception);
            return new MemberCreationResult(concurrentlyCreatedMember, false);
        }
    }

    private AuthResponseDTO.TokenResponse issueTokenResponse(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getRole());

        saveRefreshToken(member.getId(), refreshToken);

        return createTokenResponse(member, accessToken, refreshToken);
    }

    private AuthResponseDTO.TokenResponse createTokenResponse(
            Member member,
            String accessToken,
            String refreshToken
    ) {
        return AuthResponseDTO.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .memberId(member.getId())
                .onboardingCompleted(member.getOnboardingCompleted())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO.WithdrawalResponse withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        deleteMemberRelatedData(memberId);
        memberRepository.delete(member);

        return AuthResponseDTO.WithdrawalResponse.builder()
                .message(WITHDRAWAL_COMPLETE_MESSAGE)
                .build();
    }

    private void deleteMemberRelatedData(Long memberId) {
        refreshTokenStore.deleteByMemberId(memberId);
        routineGroupRepository.deleteAll(routineGroupRepository.findAllByMember_Id(memberId));
        subscriptionsRepository.deleteAllByMember_Id(memberId);
        memberTermRepository.deleteAllByMember_Id(memberId);
    }

    private AuthResponseDTO.SocialLoginResponse createSocialLoginResponse(Member member, boolean isNewMember) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getRole());

        saveRefreshToken(member.getId(), refreshToken);

        return AuthResponseDTO.SocialLoginResponse.builder()
                .memberId(member.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewMember(isNewMember)
                .onboardingCompleted(member.getOnboardingCompleted())
                .build();
    }

    private Member createSocialMember(String oauthId, String nickname, LoginType loginType) {
        return Member.builder()
                .oauthId(oauthId)
                .nickname(nickname)
                .role(Role.MEMBER)
                .loginType(loginType)
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

    private void saveRefreshToken(Long memberId, String refreshToken) {
        refreshTokenStore.save(memberId, hashRefreshToken(refreshToken), getRefreshTokenTtl(refreshToken));
    }

    private Duration getRefreshTokenTtl(String refreshToken) {
        return Duration.between(LocalDateTime.now(), jwtTokenProvider.getRefreshTokenExpiresAt(refreshToken));
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

    private record MemberCreationResult(
            Member member,
            boolean isNewMember
    ) {
    }
}
