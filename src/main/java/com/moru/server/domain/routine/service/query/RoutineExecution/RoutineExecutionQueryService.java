package com.moru.server.domain.routine.service.query.RoutineExecution;

import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;

import java.util.List;

public interface RoutineExecutionQueryService {

    List<RoutineExecutionResponseDTO.DailyExecution> getMonthlyExecutions(Long memberId, int year, int month);
}
