package com.moru.server.domain.routine.converter;

import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoutineExecutionConverter {

    public static RoutineExecution toEntity(
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req,
            Routine routine
    ){
        return RoutineExecution.builder()
                .executedDate(req.executedDate())
                .routine(routine)
                .durationSecond(req.durationSecond())
                .isCompleted(req.isCompleted())
                .memberInput(req.memberInput())
                .aiResponse(req.aiResponse())
                .actualWakeTime(req.actualWakeTime())
                .build();
    }

    public static RoutineExecutionResponseDTO.RoutineExecutionResultRes toResponse(RoutineExecution execution){
        return RoutineExecutionResponseDTO.RoutineExecutionResultRes.builder()
                .executionId(execution.getId())
                .routineId(execution.getRoutine().getId())
                .executedDate(execution.getExecutedDate())
                .isCompleted(execution.getIsCompleted())
                .durationSecond(execution.getDurationSecond())
                .build();
    }

    public static List<RoutineExecutionResponseDTO.DailyExecution> toMonthlyResponse(
            YearMonth yearMonth,
            Map<LocalDate, List<RoutineExecution>> executionsByDate
    ){
        List<RoutineExecutionResponseDTO.DailyExecution> result = new ArrayList<>();

        int daysInMonth = yearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            List<RoutineExecution> executions = executionsByDate.getOrDefault(date, List.of());

            int totalCount = executions.size();
            int completedCount = (int) executions.stream()
                    .filter(RoutineExecution::getIsCompleted)
                    .count();
            int completionRate = totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);

            result.add(RoutineExecutionResponseDTO.DailyExecution.builder()
                    .executedDate(date)
                    .completionRate(completionRate)
                    .build());
        }

        return result;
    }
}
