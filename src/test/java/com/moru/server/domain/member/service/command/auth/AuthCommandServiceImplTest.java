package com.moru.server.domain.member.service.command.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.moru.server.domain.member.client.AppleOAuthClient;
import com.moru.server.domain.member.client.AppleOAuthTokenClient;
import com.moru.server.domain.member.client.GoogleOAuthClient;
import com.moru.server.domain.member.client.KakaoOAuthClient;
import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.OAuthProvider;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.service.AppleOAuthCredentialService;
import com.moru.server.domain.member.service.MemberWithdrawalService;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import com.moru.server.global.security.jwt.JwtTokenProvider;
import com.moru.server.global.security.jwt.RefreshTokenStore;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private AppleOAuthClient appleOAuthClient;

    @Mock
    private AppleOAuthTokenClient appleOAuthTokenClient;

    @Mock
    private AppleOAuthCredentialService appleOAuthCredentialService;

    @Mock
    private MemberWithdrawalService memberWithdrawalService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthCommandServiceImpl authCommandService;

    @Test
    void reissuesTokenWhenStoredTokenMatches() {
        Member member = Member.builder()
                .id(1L)
                .oauthId("google-member-id")
                .nickname("모루")
                .role(Role.MEMBER)
                .loginType(LoginType.GOOGLE)
                .build();
        when(jwtTokenProvider.getMemberIdFromRefreshToken("refresh-token")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(1L, Role.MEMBER)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L, Role.MEMBER)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiresAt("new-refresh-token"))
                .thenReturn(LocalDateTime.now().plusDays(14));
        when(refreshTokenStore.rotate(eq(1L), anyString(), anyString(), any())).thenReturn(true);

        AuthResponseDTO.TokenResponse response = authCommandService.reissueToken("refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenStore)
                .rotate(eq(1L), anyString(), anyString(), any());
    }

    @Test
    void rejectsExpiredRefreshTokenBeforeAccessingRedis() {
        doThrow(new BusinessException(ErrorStatus.REFRESH_TOKEN_EXPIRED))
                .when(jwtTokenProvider)
                .validateRefreshToken("expired-refresh-token");

        assertThatThrownBy(() -> authCommandService.reissueToken("expired-refresh-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.REFRESH_TOKEN_EXPIRED)
                );
        verify(refreshTokenStore, times(0)).rotate(any(), anyString(), anyString(), any());
    }

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
        when(jwtTokenProvider.getRefreshTokenExpiresAt("refresh-token"))
                .thenReturn(LocalDateTime.of(2026, 8, 6, 0, 0));

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

    @Test
    void exchangesAndStoresAppleRefreshTokenBeforeIssuingServiceTokens() {
        Member member = Member.builder()
                .id(1L)
                .oauthId("apple-member-id")
                .nickname("moru@example.com")
                .role(Role.MEMBER)
                .loginType(LoginType.APPLE)
                .build();
        when(appleOAuthClient.getMemberInfo("apple-identity-token"))
                .thenReturn(new AppleOAuthClient.AppleMemberInfo("apple-member-id", "moru@example.com"));
        when(appleOAuthTokenClient.exchangeAuthorizationCode("apple-authorization-code"))
                .thenReturn(new AppleOAuthTokenClient.AppleTokens("apple-refresh-token", "exchanged-id-token"));
        when(appleOAuthClient.getMemberInfo("exchanged-id-token"))
                .thenReturn(new AppleOAuthClient.AppleMemberInfo("apple-member-id", "moru@example.com"));
        when(memberRepository.findByLoginTypeAndOauthId(LoginType.APPLE, "apple-member-id"))
                .thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(1L, Role.MEMBER)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L, Role.MEMBER)).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiresAt("refresh-token"))
                .thenReturn(LocalDateTime.now().plusDays(14));

        AuthResponseDTO.SocialLoginResponse response = authCommandService.loginWithSocial(
                OAuthProvider.APPLE,
                new AuthRequestDTO.SocialLoginRequest(
                        "apple-identity-token",
                        "apple-authorization-code"
                )
        );

        assertThat(response.memberId()).isEqualTo(1L);
        verify(appleOAuthCredentialService).saveOrUpdate(1L, "apple-refresh-token");
    }

    @Test
    void rejectsAppleLoginWhenAuthorizationCodeBelongsToAnotherMember() {
        when(appleOAuthClient.getMemberInfo("apple-identity-token"))
                .thenReturn(new AppleOAuthClient.AppleMemberInfo("apple-member-id", "moru@example.com"));
        when(appleOAuthTokenClient.exchangeAuthorizationCode("apple-authorization-code"))
                .thenReturn(new AppleOAuthTokenClient.AppleTokens("apple-refresh-token", "exchanged-id-token"));
        when(appleOAuthClient.getMemberInfo("exchanged-id-token"))
                .thenReturn(new AppleOAuthClient.AppleMemberInfo("different-member-id", "other@example.com"));

        assertThatThrownBy(() -> authCommandService.loginWithSocial(
                OAuthProvider.APPLE,
                new AuthRequestDTO.SocialLoginRequest(
                        "apple-identity-token",
                        "apple-authorization-code"
                )
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.INVALID_TOKEN));

        verify(appleOAuthCredentialService, never()).saveOrUpdate(any(), anyString());
    }

    @Test
    void delegatesWithdrawalToMemberWithdrawalService() {
        Long memberId = 1L;
        AuthResponseDTO.WithdrawalResponse expected = AuthResponseDTO.WithdrawalResponse.builder()
                .status(AuthResponseDTO.WithdrawalStatus.COMPLETED)
                .message("회원 탈퇴가 완료되었습니다.")
                .build();
        when(memberWithdrawalService.withdraw(memberId)).thenReturn(expected);

        AuthResponseDTO.WithdrawalResponse actual = authCommandService.withdraw(memberId);

        assertThat(actual).isEqualTo(expected);
        verify(memberWithdrawalService).withdraw(memberId);
    }
}
