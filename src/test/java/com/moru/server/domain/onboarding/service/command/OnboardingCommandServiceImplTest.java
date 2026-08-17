package com.moru.server.domain.onboarding.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.onboarding.dto.OnboardingRequestDTO;
import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@ExtendWith(MockitoExtension.class)
class OnboardingCommandServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ROUTINE_GROUP_ID = 15L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoutineGroupRepository routineGroupRepository;

    @InjectMocks
    private OnboardingCommandServiceImpl onboardingCommandService;

    @Test
    void completesOnboardingWhenMemberOwnsRoutineGroup() {
        Member member = createMember(false);
        OnboardingRequestDTO.CompleteRequest request = createRequest();
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(member));
        when(routineGroupRepository.existsByIdAndMember_Id(ROUTINE_GROUP_ID, MEMBER_ID))
                .thenReturn(true);

        OnboardingResponseDTO.StatusResponse response =
                onboardingCommandService.completeOnboarding(MEMBER_ID, request);

        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(member.getOnboardingCompleted()).isTrue();
    }

    @Test
    void returnsSuccessWhenOnboardingIsAlreadyCompleted() {
        Member member = createMember(true);
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(member));

        OnboardingResponseDTO.StatusResponse response =
                onboardingCommandService.completeOnboarding(MEMBER_ID, createRequest());

        assertThat(response.onboardingCompleted()).isTrue();
        verify(routineGroupRepository, never())
                .existsByIdAndMember_Id(ROUTINE_GROUP_ID, MEMBER_ID);
    }

    @Test
    void rejectsRoutineGroupNotOwnedByMember() {
        Member member = createMember(false);
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(member));
        when(routineGroupRepository.existsByIdAndMember_Id(ROUTINE_GROUP_ID, MEMBER_ID))
                .thenReturn(false);

        assertThatThrownBy(() ->
                onboardingCommandService.completeOnboarding(MEMBER_ID, createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode())
                                .isEqualTo(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        assertThat(member.getOnboardingCompleted()).isFalse();
    }

    @Test
    void rejectsMissingMember() {
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                onboardingCommandService.completeOnboarding(MEMBER_ID, createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode())
                                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private OnboardingRequestDTO.CompleteRequest createRequest() {
        return new OnboardingRequestDTO.CompleteRequest(ROUTINE_GROUP_ID);
    }

    private Member createMember(boolean onboardingCompleted) {
        return Member.builder()
                .id(MEMBER_ID)
                .oauthId("google-member-id")
                .nickname("모루")
                .role(Role.MEMBER)
                .loginType(LoginType.GOOGLE)
                .onboardingCompleted(onboardingCompleted)
                .build();
    }
}
