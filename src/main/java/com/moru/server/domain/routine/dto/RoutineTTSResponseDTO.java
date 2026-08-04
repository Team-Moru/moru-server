package com.moru.server.domain.routine.dto;

import com.moru.server.domain.routine.entity.enums.RoutineType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record RoutineTTSResponseDTO() {

    @Builder
    @Schema(description = "루틴 TTS 응답")
    public record RoutineTTSRes(

            @Schema(description = "루틴 ID", example = "14")
            Long routineId,

            @Schema(description = "루틴 제목", example = "스트레칭하기")
            String title,

            @Schema(description = "루틴 타입", example = "TIMER")
            RoutineType type,

            @Schema(description = "루틴 내 TTS 스텝 목록")
            List<RoutineTTSStep> steps
    ) {
        @Builder
        @Schema(description = "루틴 TTS 스텝 상세 정보")
        public record RoutineTTSStep(

                @Schema(description = "스텝 ID", example = "101")
                Long stepId,

                @Schema(description = "스텝 원본 텍스트", example = "목 스트레칭")
                String content,

                @Schema(description = "실제 합성된 음성의 문장. 합성 전(PENDING)이거나 실패 시 null",
                        example = "이제 목을 부드럽게 풀어볼까요?", nullable = true)
                String ttsIntro,

                @Schema(description = "TTS 생성 상태 (PENDING, COMPLETED, FAILED)", example = "COMPLETED")
                String ttsStatus,

                @Schema(description = "음원 파일 S3 URL", example = "https://.../101.mp3", nullable = true)
                String s3Url
        ) {}
    }
}
