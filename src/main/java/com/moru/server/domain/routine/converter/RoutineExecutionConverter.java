package com.moru.server.domain.routine.converter;

import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;

import java.time.LocalDate;
import java.util.Comparator;
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
            Map<LocalDate, List<RoutineExecution>> executionsByDate
    ){
        return executionsByDate.entrySet().stream()
                .map(entry -> {
                    List<RoutineExecution> executions = entry.getValue();
                    int totalCount = executions.size();
                    int completedCount = (int) executions.stream()
                            .filter(RoutineExecution::getIsCompleted)
                            .count();
                    int completionRate = totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);

                    return RoutineExecutionResponseDTO.DailyExecution.builder()
                            .executedDate(entry.getKey())
                            .completionRate(completionRate)
                            .build();
                })
                .sorted(Comparator.comparing(RoutineExecutionResponseDTO.DailyExecution::executedDate))
                .toList();
    }
}
