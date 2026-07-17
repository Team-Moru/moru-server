package com.moru.server.domain.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingResponseDTO() {

    @Schema(description = "온보딩 완료 여부 조회 응답")
    public record StatusResponse(
            @Schema(description = "온보딩 완료 여부", example = "false")
            Boolean onboardingCompleted
    ) {
    }
}
