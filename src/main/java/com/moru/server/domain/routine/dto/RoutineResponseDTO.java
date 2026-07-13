package com.moru.server.domain.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record RoutineResponseDTO() {

    @Builder
    @Schema(description = "루틴 삭제 응답")
    public record DeleteResponse(
            @Schema(description = "루틴 ID", example = "104")
            Long routineId
    ) {
    }
}
