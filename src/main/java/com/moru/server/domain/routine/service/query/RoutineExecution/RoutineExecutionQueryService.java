package com.moru.server.domain.routine.service.query.RoutineExecution;

import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface RoutineExecutionQueryService {

    List<RoutineExecutionResponseDTO.DailyExecution> getMonthlyExecutions(Long memberId, int year, int month);

    RoutineExecutionResponseDTO.WeeklyReportResponse getWeeklyReport(Long memberId);

    RoutineExecutionResponseDTO.WakePatternResponse getWakePattern(Long memberId);
    RoutineExecutionResponseDTO.DailyResponse getDailyResponse(Long memberId, LocalDate executedDate);
}
