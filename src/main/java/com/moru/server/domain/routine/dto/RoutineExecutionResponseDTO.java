package com.moru.server.domain.routine.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

public record RoutineExecutionResponseDTO() {

    @Builder
    @Schema(description ="루틴 실행 결과 저장 응답")
    public record RoutineExecutionResultRes(

            @Schema(description = "실행 날짜", example = "2026-07-13")
            LocalDate executedDate,

            @Schema(description = "저장된 실행 결과 ID", example = "1")
            Long executionId,

            @Schema(description = "루틴 ID", example = "1")
            Long routineId,

            @Schema(description = "실행 소요 시간(초)", example = "20")
            Integer durationSecond,

            @Schema(description = "완료 여부", example = "true")
            Boolean isCompleted
    ){}

    @Builder
    @Schema(description = "일별 루틴 실행 달성률")
    public record DailyExecution(

            @Schema(description = "실행 날짜", example = "2026-04-01")
            LocalDate executedDate,

            @Schema(description = "실행 달성률(%)", example = "80")
            Integer completionRate
    ){}

    @Builder
    @Schema(description ="루틴 AI 확인 결과 응답")
    public record AiResponseRes(
            @Schema(description = "AI 응답 상세 내역", example = "다음으로 넘어갈게요 !")
            String aiResponse,

            @Schema(description = "진행 여부", example = "true")
            Boolean shouldProceed
    ) {}
}
