package com.moru.server.domain.routine.dto;

import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import com.moru.server.domain.routine.entity.enums.RoutineType;

public record RoutineGroupResponseDTO() {

    @Builder
    @Schema(description = "루틴 그룹 상세 응답")
    public record DetailResponse(
            @Schema(description = "루틴 그룹 ID", example = "10")
            Long routineGroupId,

            @Schema(description = "루틴 그룹 제목", example = "활력 루틴")
            String title,

            @Schema(description = "루틴 그룹 설명")
            String description,

            @Schema(description = "알람 요일", example = "MON,TUE,WED,THU,FRI")
            String alarmDays,

            @Schema(description = "알람 시간", example = "09:00")
            LocalTime alarmTime,

            @Schema(description = "날씨 알림 활성화 여부", example = "true")
            Boolean weatherNotificationEnabled,

            @Schema(description = "루틴 목록")
            List<RoutineResponse> routines
    ) {
    }

    @Builder
    @Schema(description = "루틴 응답")
    public record RoutineResponse(
            @Schema(description = "루틴 ID", example = "101")
            Long routineId,

            @Schema(description = "루틴 제목", example = "잠자리 정리하기")
            String title,

            @Schema(description = "루틴 타입", example = "CHECK")
            RoutineType type,

            @Schema(description = "소요 시간(초)")
            Integer durationSecond
    ) {
    }

    @Builder
    @Schema(description = "루틴 그룹 삭제 응답")
    public record DeleteResponse(
            @Schema(description = "루틴 그룹 ID", example = "1")
            Long routineGroupId
    ) {
    }
  

    @Builder
    @Schema(description = "루틴 그룹 활성화 토글 응답")
    public record ActiveResponse(
            @Schema(description = "루틴 그룹 ID", example = "11")
            Long routineGroupId,

            @Schema(description = "활성화 여부", example = "true")
            Boolean isActive
    ) {
    }
  
  
    @Builder
    @Schema(description = "루틴 그룹 목록 조회 응답")
    public record SummaryResponse(
            @Schema(description = "루틴 그룹 ID", example = "10")
            Long routineGroupId,

            @Schema(description = "루틴 그룹 제목", example = "활력 루틴")
            String title,

            @Schema(description = "활성화 여부", example = "true")
            Boolean isActive,

            @Schema(description = "루틴 개수", example = "6")
            Integer routineCount,

            @Schema(description = "총 소요 시간(초)", example = "900")
            Integer totalDurationSecond
    ) {
    }

    @Builder
    @Schema(description = "오늘의 루틴 진행 현황 응답")
    public record TodayResponse(
            @Schema(description = "완료한 항목 수", example = "3")
            Integer completedCount,

            @Schema(description = "전체 항목 수", example = "6")
            Integer totalCount,

            @Schema(description = "완료율(%)", example = "50")
            Integer completionRate
    ) {
    }
}
