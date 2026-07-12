package com.moru.server.domain.routine.service.command.Routine;

import com.moru.server.domain.routine.dto.RoutineResponseDTO;

public interface RoutineCommandService {
    RoutineResponseDTO.DeleteResponse deleteRoutine(Long memberId, Long routineId);
}
