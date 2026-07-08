package com.moru.server.domain.routine.dto;

import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import com.moru.server.domain.routine.entity.enums.RoutineType;

public record RoutineGroupResponseDTO() {

    @Builder
    @Schema(description = "루틴 그룹 생성 응답")
    public record CreateResponse(
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
}
