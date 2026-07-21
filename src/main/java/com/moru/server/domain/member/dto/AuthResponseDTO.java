package com.moru.server.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record AuthResponseDTO() {

    @Builder
    @Schema(description = "토큰 발급 응답")
    public record TokenResponse(
            @Schema(description = "Access Token")
            String accessToken,

            @Schema(description = "Refresh Token")
            String refreshToken,

            @Schema(description = "토큰 타입", example = "Bearer")
            String tokenType,

            @Schema(description = "회원 ID", example = "1")
            Long memberId,

            @Schema(description = "온보딩 완료 여부", example = "false")
            Boolean onboardingCompleted
    ) {
    }

    @Builder
    @Schema(description = "회원탈퇴 응답")
    public record WithdrawalResponse(
            @Schema(description = "회원탈퇴 완료 메시지", example = "회원 탈퇴가 완료되었습니다.")
            String message
    ) {
    }
}
