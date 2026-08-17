package com.moru.server.domain.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OnboardingRequestDTO() {

    @Schema(description = "온보딩 완료 요청")
    public record CompleteRequest(
            @NotNull(message = "routineGroupId는 필수입니다.")
            @Schema(description = "동기화가 완료된 회원 소유 루틴 그룹 ID", example = "15")
            Long routineGroupId
    ) {
    }
}
