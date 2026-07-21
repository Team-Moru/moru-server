package com.moru.server.domain.member.service.command.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.repository.MemberTermRepository;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.domain.subscriptions.repository.SubscriptionsRepository;
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
    private static final String WITHDRAWAL_COMPLETE_MESSAGE = "회원 탈퇴가 완료되었습니다.";

    private final MemberRepository memberRepository;
    private final MemberTermRepository memberTermRepository;
    private final RoutineGroupRepository routineGroupRepository;
    private final SubscriptionsRepository subscriptionsRepository;
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
        routineGroupRepository.deleteAll(routineGroupRepository.findAllByMember_Id(memberId));
        subscriptionsRepository.deleteAllByMember_Id(memberId);
        memberTermRepository.deleteAllByMember_Id(memberId);
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
