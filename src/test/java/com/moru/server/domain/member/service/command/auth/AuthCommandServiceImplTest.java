package com.moru.server.domain.member.service.command.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
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
import com.moru.server.domain.member.repository.MemberTermRepository;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.domain.subscriptions.repository.SubscriptionsRepository;
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
    private MemberTermRepository memberTermRepository;

    @Mock
    private RoutineGroupRepository routineGroupRepository;

    @Mock
    private SubscriptionsRepository subscriptionsRepository;

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
    void deletesRedisRefreshTokenAfterMemberDeletionIsFlushed() {
        Long memberId = 1L;
        Member member = Member.builder()
                .id(memberId)
                .oauthId("google-member-id")
                .nickname("모루")
                .role(Role.MEMBER)
                .loginType(LoginType.GOOGLE)
                .build();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(routineGroupRepository.findAllByMember_Id(memberId)).thenReturn(List.of());

        authCommandService.withdraw(memberId);

        InOrder inOrder = inOrder(memberRepository, refreshTokenStore);
        inOrder.verify(memberRepository).delete(member);
        inOrder.verify(memberRepository).flush();
        inOrder.verify(refreshTokenStore).deleteByMemberId(memberId);
    }

    @Test
    void doesNotDeleteRedisRefreshTokenWhenDatabaseDeletionFails() {
        Long memberId = 1L;
        Member member = Member.builder()
                .id(memberId)
                .oauthId("google-member-id")
                .nickname("모루")
                .role(Role.MEMBER)
                .loginType(LoginType.GOOGLE)
                .build();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(routineGroupRepository.findAllByMember_Id(memberId)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("member deletion failed"))
                .when(memberRepository)
                .flush();

        assertThatThrownBy(() -> authCommandService.withdraw(memberId))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(refreshTokenStore, never()).deleteByMemberId(memberId);
    }
}
