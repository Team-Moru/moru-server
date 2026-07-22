package com.moru.server.domain.routine.service.command.RoutineExecution;

import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;

public interface RoutineExecutionCommandService {

    RoutineExecutionResponseDTO.RoutineExecutionResultRes saveExecutionResult(Long memberId, RoutineExecutionRequestDTO.RoutineExecutionResultReq req);
    RoutineExecutionResponseDTO.AiResponseRes judgeUserResponse(Long memberId, RoutineExecutionRequestDTO.AiResponseReq req );
}
