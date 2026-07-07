package com.moru.server.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AuthRequestDTO() {

    @Schema(description = "개발용 임시 토큰 발급 요청")
    public record DevTokenRequest(
        @NotBlank(message = "oauthId는 필수입니다.")
        @Schema(description = "테스트 회원 식별자", example = "test-user-1")
        String oauthId,

        @Schema(description = "테스트 회원 닉네임", example = "이름")
        String nickname
    ) {
    }
}
