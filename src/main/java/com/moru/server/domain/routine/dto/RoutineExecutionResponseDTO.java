package com.moru.server.domain.routine.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

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
    @Schema(description = "주간 리포트 응답")
    public record WeeklyReportResponse(

            @Schema(description = "이번 주 완료율(%)", example = "75")
            Integer completionRate,

            @Schema(description = "지난주 대비 증감(%). 감소 시 음수", example = "-10")
            Integer completionRateDiff,

            @Schema(description = "총 소요시간(초)", example = "3600")
            Integer totalDurationSecond,

            @Schema(description = "요일별 완료율 목록 (월~일, 7개 고정)")
            List<DailyCompletionRate> weeklyCompletionRate,

            @Schema(description = "루틴별 완료율 목록")
            List<RoutineStat> routineStats
    ){}

    @Builder
    @Schema(description = "요일별 완료율")
    public record DailyCompletionRate(

            @Schema(description = "요일", example = "MON")
            String day,

            @Schema(description = "해당 요일 완료율(%). 아직 지나지 않았거나 실행 이력이 없는 요일이면 null", example = "80", nullable = true)
            Integer completionRate
    ){}

    @Builder
    @Schema(description = "루틴별 완료율")
    public record RoutineStat(

            @Schema(description = "루틴 ID", example = "1")
            Long routineId,

            @Schema(description = "루틴명", example = "물 마시기")
            String title,

            @Schema(description = "항목 완료율(%)", example = "60")
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
