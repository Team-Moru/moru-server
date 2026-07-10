package com.moru.server.domain.routine.service.query.RoutineGroup;

import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;

public interface RoutineGroupQueryService {
    RoutineGroupResponseDTO.DetailResponse getRoutineGroupDetail(Long memberId, Long routineGroupId);
    List<RoutineGroupResponseDTO.SummaryResponse> getRoutineGroups(Long memberId);
}
