package com.moru.server.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class AuthResponseDTO {

    @Getter
    @Builder
    @Schema(description = "토큰 발급 응답")
    public static class TokenResponse {

        @Schema(description = "Access Token")
        private String accessToken;

        @Schema(description = "Refresh Token")
        private String refreshToken;

        @Schema(description = "토큰 타입", example = "Bearer")
        private String tokenType;

        @Schema(description = "회원 ID", example = "1")
        private Long memberId;

        @Schema(description = "온보딩 완료 여부", example = "false")
        private Boolean onboardingCompleted;
    }
}
