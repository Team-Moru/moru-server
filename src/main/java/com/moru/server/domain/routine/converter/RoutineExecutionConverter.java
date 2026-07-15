package com.moru.server.domain.routine.converter;

import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;

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
}
