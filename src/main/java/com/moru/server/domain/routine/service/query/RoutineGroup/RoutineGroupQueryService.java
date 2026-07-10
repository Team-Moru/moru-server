package com.moru.server.domain.routine.service.query.RoutineGroup;

import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;

import java.util.List;

public interface RoutineGroupQueryService {
    List<RoutineGroupResponseDTO.SummaryResponse> getRoutineGroups(Long memberId);
}
