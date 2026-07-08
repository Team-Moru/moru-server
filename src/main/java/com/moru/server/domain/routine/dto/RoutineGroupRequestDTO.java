package com.moru.server.domain.routine.dto;

import com.moru.server.domain.routine.entity.enums.RoutineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalTime;
import java.util.List;

public record RoutineGroupRequestDTO() {


    @Schema(description = "루틴 그룹 생성 요청", example = """
        {
          "title": "활력 루틴",
          "description": null,
          "alarmDays": "MON,TUE,WED,THU,FRI",
          "alarmTime": "09:00",
          "weatherNotificationEnabled": true,
          "routines": [
            { "title": "잠자리 정리하기", "type": "CHECK", "durationSecond": null },
            { "title": "심호흡하며 명상하기", "type": "TIMER", "durationSecond": 180 },
            { "title": "오늘의 다짐 확언하기", "type": "INPUT", "durationSecond": 60 }
          ]
        }
        """)
    public record CreateRequest(
            @NotBlank(message = "title은 필수입니다.")
            @Schema(description = "루틴 그룹 제목", example = "활력 루틴")
            String title,

            @Schema(description = "루틴 그룹 설명", example = "아침에 활력을 주는 루틴")
            String description,

            @Schema(description = "알람 요일 (콤마 구분)", example = "MON,TUE,WED,THU,FRI")
            String alarmDays,

            @Schema(description = "알람 시간", example = "09:00")
            LocalTime alarmTime,

            @NotNull(message = "weatherNotificationEnabled는 필수입니다.")
            @Schema(description = "날씨 알림 활성화 여부", example = "true")
            Boolean weatherNotificationEnabled,

            @NotNull(message = "routines 값은 필수입니다.")
            @Valid
            @Schema(description = "루틴 목록")
            List<RoutineRequest> routines
    ) {
    }

    @Schema(description = "루틴 생성 요청")
    public record RoutineRequest(
            @NotBlank(message = "title은 필수입니다.")
            @Schema(description = "루틴 제목", example = "잠자리 정리하기")
            String title,

            @NotNull(message = "type은 필수입니다.")
            @Schema(description = "루틴 타입", example = "CHECK")
            RoutineType type,


            @NotNull(message = "durationSecond는 필수입니다.")
            @PositiveOrZero(message = "durationSecond는 0 이상이어야 합니다.")
            @Schema(description = "타이머/입력 소요 시간(초)", example = "180")
            Integer durationSecond
    ) {
    }
}