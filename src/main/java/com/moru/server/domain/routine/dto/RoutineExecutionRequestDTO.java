package com.moru.server.domain.routine.dto;

import com.moru.server.domain.routine.entity.enums.RoutineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record RoutineExecutionRequestDTO() {

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

    @Schema(description = "확인형 루틴 사용자 응답 AI 판단 요청")
    public record AiResponseReq(
            @NotNull(message = "routineId는 필수입니다.")
            @Schema(description = "루틴 ID", example = "1")
            Long routineId,

            @NotNull(message = "executedDate는 필수입니다.")
            @Schema(description = "실행 날짜", example = "2026-07-13")
            LocalDate executedDate,

            @PositiveOrZero(message = "durationSecond는 0 이상이어야 합니다.")
            @Schema(description = "실행 소요 시간(초)", example = "20")
            Integer durationSecond,

            @NotNull(message = "memberInput는 필수입니다.")
            @Size(max = 500, message = "memberInput은 500자 이하여야 합니다.")
            @Schema(description = "사용자 입력", example = "응, 다음")
            String memberInput,

            @Schema(description = "실제 기상 시간 (마지막 루틴만 전송)", example = "07:23")
            LocalTime actualWakeTime,

            @Size(max = 100, message = "clientExecutionId는 100자 이하여야 합니다.")
            @Schema(
                    description = """
                            중복 요청 방지 키. 네트워크 재전송 시 이 값과 요청 본문을 모두 그대로 보내면 \
                            AI를 다시 호출하지 않고 최초 결과를 반환합니다. \
                            앞선 동일 요청이 아직 처리 중이면 409(COMMON409)를 반환합니다. \
                            사용자가 답변을 수정해 다시 보낼 때는 반드시 새 값을 발급해야 합니다 \
                            (같은 값에 다른 본문이 오면 409(COMMON410)). \
                            보내지 않으면 중복 방지 없이 매번 처리됩니다.""",
                    example = "8f14e45f-ea8f-4c1b-b6d0-1a2b3c4d5e6f",
                    nullable = true
            )
            String clientExecutionId
            ){}
}
