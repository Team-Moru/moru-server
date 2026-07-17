package com.moru.server.domain.onboarding.service.query;

import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;

public interface OnboardingQueryService {

    OnboardingResponseDTO.StatusResponse getOnboardingStatus(Long memberId);
}
