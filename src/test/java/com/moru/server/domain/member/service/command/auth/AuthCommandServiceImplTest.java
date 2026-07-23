package com.moru.server.domain.member.service.command.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private AppleOAuthClient appleOAuthClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthCommandServiceImpl authCommandService;

    @Test
    void returnsConcurrentlyCreatedMemberWhenSocialMemberInsertConflicts() {
        String oauthId = "google-member-id";
        Member existingMember = Member.builder()
                .id(1L)
                .oauthId(oauthId)
                .nickname("모루")
                .role(Role.MEMBER)
                .loginType(LoginType.GOOGLE)
                .build();

        when(googleOAuthClient.getMemberInfo("google-id-token"))
                .thenReturn(new GoogleOAuthClient.GoogleMemberInfo(oauthId, "모루"));
        when(memberRepository.findByLoginTypeAndOauthId(LoginType.GOOGLE, oauthId))
                .thenReturn(Optional.empty(), Optional.of(existingMember));
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate social member"));
        when(jwtTokenProvider.createAccessToken(1L, Role.MEMBER)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L, Role.MEMBER)).thenReturn("refresh-token");

        AuthResponseDTO.SocialLoginResponse response = authCommandService.loginWithSocial(
                OAuthProvider.GOOGLE,
                new AuthRequestDTO.SocialLoginRequest("google-id-token", null)
        );

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.isNewMember()).isFalse();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(memberRepository, times(2))
                .findByLoginTypeAndOauthId(LoginType.GOOGLE, oauthId);
    }
}
