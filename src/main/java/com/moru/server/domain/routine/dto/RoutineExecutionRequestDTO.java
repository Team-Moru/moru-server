package com.moru.server.domain.routine.dto;

import com.moru.server.domain.routine.entity.enums.RoutineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class RoutineExecutionRequestDTO {

    @Schema(description = "개별 루틴 실행 결과 저장 요청")
    public record RoutineExecutionResultReq(
            @NotNull(message = "executedDate는 필수입니다.")
            @Schema(description = "실행 날짜", example = "2026-07-13")
            LocalDate executedDate,

            @NotNull(message = "routineId는 필수입니다.")
            @Schema(description = "루틴 ID", example = "1")
            Long routineId,

            @PositiveOrZero(message = "durationSecond는 0 이상이어야 합니다.")
            @Schema(description = "실행 소요 시간(초)", example = "20")
            Integer durationSecond,

            @NotNull(message="isCompleted는 필수입니다")
            @Schema(description ="완료여부", example = "true")
            Boolean isCompleted,

            @Size(max = 500, message = "memberInput은 500자 이하여야 합니다.")
            @Schema(description = "사용자 입력 기록", example = "오늘도 화이팅")
            String memberInput,

            @Size(max = 500, message = "aiResponse는 500자 이하여야 합니다.")
            @Schema(description = "AI 생성 응답", example = "오늘의 다짐을 외쳐보세요!")
            String aiResponse,

            @Schema(description = "실제 기상 시간 (마지막 루틴만 전송)", example = "07:23")
            LocalTime actualWakeTime
    ){}

}
