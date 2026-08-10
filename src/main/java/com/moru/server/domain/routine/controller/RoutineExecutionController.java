package com.moru.server.domain.routine.controller;


import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.service.command.RoutineExecution.RoutineExecutionCommandService;
import com.moru.server.domain.routine.service.query.RoutineExecution.RoutineExecutionQueryService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.response.code.status.SuccessStatus;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;



@Tag(name = "Routine Execution", description = "루틴 실행 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routine-executions")
@Validated
public class RoutineExecutionController {

    private final RoutineExecutionCommandService routineExecutionCommandService;
    private final RoutineExecutionQueryService routineExecutionQueryService;

    @Operation(summary = "루틴 실행 결과 저장", description = "단일 루틴의 실행 결과를 저장합니다.")
    @PostMapping()
    public ApiResponse<RoutineExecutionResponseDTO.RoutineExecutionResultRes> createRoutineExecution(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RoutineExecutionRequestDTO.RoutineExecutionResultReq request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.of(SuccessStatus._CREATED,
                routineExecutionCommandService.saveExecutionResult(member.memberId(), request, idempotencyKey));
    }

    @Operation(summary = "월별 루틴 실행 이력 조회", description = "특정 연월의 날짜별 루틴 실행 달성률을 조회합니다.")
    @GetMapping("/monthly")
    public ApiResponse<List<RoutineExecutionResponseDTO.DailyExecution>> getMonthlyExecutions(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam @Min(2000) @Max(999999999) Integer year,
            @RequestParam @Min(1) @Max(12) Integer month
    ) {

        return ApiResponse.onSuccess(
                routineExecutionQueryService.getMonthlyExecutions(member.memberId(), year, month));
    }


    @Operation(summary = "주간 리포트 조회", description = "이번 주 루틴 실행 완료율, 지난주 대비 증감, 요일별/항목별 완료율을 조회합니다.")
    @GetMapping("/weekly")
    public ApiResponse<RoutineExecutionResponseDTO.WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {

        return ApiResponse.onSuccess(routineExecutionQueryService.getWeeklyReport(member.memberId()));
    }

    @Operation(summary = "기상 패턴 분석 조회", description = "최근 7일 평균 기상 시각, 지난주 대비 증감, 기상 규칙성 점수를 조회합니다. 실행 기록이 없으면 result가 null입니다.")
    @GetMapping("/wake-pattern")
    public ApiResponse<RoutineExecutionResponseDTO.WakePatternResponse> getWakePattern(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(routineExecutionQueryService.getWakePattern(member.memberId()));
    }

    @Operation(summary = "루틴 실행 AI 응답 확인", description = "사용자의 응답을 AI가 판단하고 저장합니다.")
    @PostMapping("/ai-step")
    public ApiResponse<RoutineExecutionResponseDTO.AiResponseRes> judgeRoutineExecution(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RoutineExecutionRequestDTO.AiResponseReq req
    ) {
        return ApiResponse.onSuccess(routineExecutionCommandService.judgeUserResponse(member.memberId(), req));
    }

    @Operation(summary = "오늘 루틴 완료 조회", description = "특정 날짜의 루틴 실행 결과(완수율, 스트릭, 총 소요시간, 실제 기상시간, 루틴별 상세)를 조회합니다.")
    @GetMapping("/daily/{date}")
    public ApiResponse<RoutineExecutionResponseDTO.DailyResponse> getDailyReport(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.onSuccess(routineExecutionQueryService.getDailyResponse(member.memberId(), date));
    }

}
