package com.moru.server.domain.routine.service.command.RoutineGroup;

import com.moru.server.domain.routine.dto.RoutineGroupRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;

public interface RoutineGroupCommandService {
    RoutineGroupResponseDTO.DetailResponse createRoutineGroup(Long memberId, RoutineGroupRequestDTO.CreateRequest request);
}
