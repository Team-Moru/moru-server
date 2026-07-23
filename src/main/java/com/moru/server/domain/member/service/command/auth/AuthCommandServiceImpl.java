package com.moru.server.domain.member.service.command.auth;

import java.util.Optional;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
import com.moru.server.global.security.jwt.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AuthCommandServiceImpl implements AuthCommandService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String DEFAULT_DEV_NICKNAME = "테스트회원";
    private static final LoginType DEFAULT_DEV_LOGIN_TYPE = LoginType.KAKAO;

    private final MemberRepository memberRepository;
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

        return AuthResponseDTO.TokenResponse.builder()
                .accessToken(jwtTokenProvider.createAccessToken(member.getId(), member.getRole()))
                .refreshToken(jwtTokenProvider.createRefreshToken(member.getId(), member.getRole()))
                .tokenType(TOKEN_TYPE)
                .memberId(member.getId())
                .onboardingCompleted(member.getOnboardingCompleted())
                .build();
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

    private AuthResponseDTO.SocialLoginResponse createSocialLoginResponse(Member member, boolean isNewMember) {
        return AuthResponseDTO.SocialLoginResponse.builder()
                .memberId(member.getId())
                .accessToken(jwtTokenProvider.createAccessToken(member.getId(), member.getRole()))
                .refreshToken(jwtTokenProvider.createRefreshToken(member.getId(), member.getRole()))
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
