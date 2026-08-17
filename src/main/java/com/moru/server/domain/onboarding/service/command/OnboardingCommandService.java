package com.moru.server.domain.onboarding.service.command;

import com.moru.server.domain.onboarding.dto.OnboardingRequestDTO;
import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;

public interface OnboardingCommandService {

    OnboardingResponseDTO.StatusResponse completeOnboarding(
            Long memberId,
            OnboardingRequestDTO.CompleteRequest request
    );
}
