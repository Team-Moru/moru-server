package com.moru.server.domain.onboarding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.onboarding.dto.OnboardingRequestDTO;
import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;
import com.moru.server.domain.onboarding.service.command.OnboardingCommandService;
import com.moru.server.domain.onboarding.service.query.OnboardingQueryService;
import com.moru.server.domain.routine.service.query.RoutineGroup.RoutineGroupQueryService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private OnboardingQueryService onboardingQueryService;

    @Mock
    private OnboardingCommandService onboardingCommandService;

    @Mock
    private RoutineGroupQueryService routineGroupQueryService;

    @InjectMocks
    private OnboardingController onboardingController;

    @Test
    void delegatesOnboardingCompletionToCommandService() {
        AuthenticatedMember member = new AuthenticatedMember(1L, Role.MEMBER);
        OnboardingRequestDTO.CompleteRequest request =
                new OnboardingRequestDTO.CompleteRequest(15L);
        OnboardingResponseDTO.StatusResponse serviceResponse =
                new OnboardingResponseDTO.StatusResponse(true);
        when(onboardingCommandService.completeOnboarding(1L, request))
                .thenReturn(serviceResponse);

        ApiResponse<OnboardingResponseDTO.StatusResponse> response =
                onboardingController.completeOnboarding(member, request);

        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getResult().onboardingCompleted()).isTrue();
        verify(onboardingCommandService).completeOnboarding(1L, request);
    }
}
