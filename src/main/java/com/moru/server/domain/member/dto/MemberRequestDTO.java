package com.moru.server.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MemberRequestDTO() {

    @Schema(description = "목소리 타입 변경 요청")
    public record TtsUpdateRequest(
            @NotNull(message = "ttsId는 필수입니다.")
            @Schema(description = "변경할 목소리 타입 ID", example = "1")
            Long ttsId
    ) {
    }
}
