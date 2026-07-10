package com.moru.server.domain.routine.service.command.RoutineGroup;

import com.moru.server.domain.routine.dto.RoutineGroupRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;

public interface RoutineGroupCommandService {
    RoutineGroupResponseDTO.CreateResponse createRoutineGroup(Long memberId, RoutineGroupRequestDTO.CreateRequest request);
    RoutineGroupResponseDTO.ActiveResponse updateActive(Long memberId, Long routineGroupId, RoutineGroupRequestDTO.ActiveRequest request);
}
