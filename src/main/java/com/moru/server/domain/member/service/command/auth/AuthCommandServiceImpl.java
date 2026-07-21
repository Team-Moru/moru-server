package com.moru.server.domain.member.service.command.auth;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.client.KakaoOAuthClient;
import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.OAuthProvider;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
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

    private final MemberRepository memberRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponseDTO.TokenResponse issueDevToken(AuthRequestDTO.DevTokenRequest request) {
        Member member = memberRepository.findByOauthId(request.oauthId())
                .orElseGet(() -> memberRepository.save(createDevMember(request)));

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
        throw new BusinessException(ErrorStatus.INVALID_TOKEN);
    }

    private AuthResponseDTO.SocialLoginResponse loginWithApple(AuthRequestDTO.SocialLoginRequest request) {
        throw new BusinessException(ErrorStatus.INVALID_TOKEN);
    }

    private AuthResponseDTO.SocialLoginResponse loginOrCreateMember(
            String oauthId,
            String nickname,
            LoginType loginType
    ) {
        Optional<Member> existingMember = memberRepository.findByOauthId(oauthId);
        boolean isNewMember = existingMember.isEmpty();
        Member member = existingMember.orElseGet(
                () -> memberRepository.save(createSocialMember(oauthId, nickname, loginType))
        );

        return createSocialLoginResponse(member, isNewMember);
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

}
