package com.moru.server.domain.routine.service.query.RoutineGroup;

import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.enums.RoutineGoalType;

import java.util.List;

public interface RoutineGroupQueryService {
    RoutineGroupResponseDTO.DetailResponse getRoutineGroupDetail(Long memberId, Long routineGroupId);
    List<RoutineGroupResponseDTO.SummaryResponse> getRoutineGroups(Long memberId);
    RoutineGroupResponseDTO.TodayResponse getTodayRoutine(Long memberId);
    RoutineGroupResponseDTO.ActiveRoutineResponse getActiveRoutine(Long memberId);

    List<RoutineGroupResponseDTO.DetailResponse> getRecommendedRoutineGroups(RoutineGoalType goalType);
}
