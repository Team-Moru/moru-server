package com.moru.server.domain.routine.converter;


import java.util.List;
import java.util.Map;

import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;
import com.moru.server.domain.routine.entity.RoutineGroup;

public class RoutineGroupConverter {

    public static RoutineGroupResponseDTO.DetailResponse toDetailResponse(RoutineGroup routineGroup) {
        List<RoutineGroupResponseDTO.RoutineResponse> routineResponses = routineGroup.getRoutines().stream()
                .map(RoutineGroupConverter::toRoutineResponse)
                .toList();

        return RoutineGroupResponseDTO.DetailResponse.builder()
                .routineGroupId(routineGroup.getId())
                .title(routineGroup.getTitle())
                .description(routineGroup.getDescription())
                .alarmDays(routineGroup.getAlarmDays())
                .alarmTime(routineGroup.getAlarmTime())
                .weatherNotificationEnabled(routineGroup.getWeatherNotificationEnabled())
                .routines(routineResponses)
                .build();
    }

    public static RoutineGroupResponseDTO.RoutineResponse toRoutineResponse(Routine routine) {
        return RoutineGroupResponseDTO.RoutineResponse.builder()
                .routineId(routine.getId())
                .title(routine.getTitle())
                .type(routine.getType())
                .durationSecond(routine.getTimer())
                .build();
    }

    public static RoutineGroupResponseDTO.DeleteResponse toDeleteResponse(Long routineGroupId) {
        return RoutineGroupResponseDTO.DeleteResponse.builder()
                .routineGroupId(routineGroupId)
                .build();
    }
  
    public static RoutineGroupResponseDTO.ActiveResponse toActiveResponse(RoutineGroup routineGroup) {
        return RoutineGroupResponseDTO.ActiveResponse.builder()
                .routineGroupId(routineGroup.getId())
                .isActive(routineGroup.getIsActive())
                .build();
    }
     
          
    public static RoutineGroupResponseDTO.SummaryResponse toSummaryResponse(RoutineGroup routineGroup) {
        return RoutineGroupResponseDTO.SummaryResponse.builder()
                .routineGroupId(routineGroup.getId())
                .title(routineGroup.getTitle())
                .isActive(routineGroup.getIsActive())
                .routineCount(routineGroup.getRoutineCount())
                .totalDurationSecond(routineGroup.getTotalDurationSecond())
                .build();
    }

    public static RoutineGroupResponseDTO.TodayResponse toTodayResponse(int completedCount, int totalCount) {
        int completionRate = totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);

        return RoutineGroupResponseDTO.TodayResponse.builder()
                .completedCount(completedCount)
                .totalCount(totalCount)
                .completionRate(completionRate)
                .build();
    }

    public static RoutineGroupResponseDTO.ActiveRoutineResponse toActiveRoutineResponse(
            RoutineGroup routineGroup, Map<Long, RoutineExecution> executionByRoutineId) {
        List<RoutineGroupResponseDTO.RoutineItem> routineItems = routineGroup.getRoutines().stream()
                .map(routine -> toRoutineItem(routine, executionByRoutineId.get(routine.getId())))
                .toList();

        int totalCount = routineGroup.getRoutineCount();
        int completedCount = (int) routineItems.stream()
                .filter(RoutineGroupResponseDTO.RoutineItem::isCompleted)
                .count();
        int completionRate = totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);

        return RoutineGroupResponseDTO.ActiveRoutineResponse.builder()
                .routineGroupId(routineGroup.getId())
                .title(routineGroup.getTitle())
                .totalDurationSec(routineGroup.getTotalDurationSecond())
                .completionRate(completionRate)
                .routines(routineItems)
                .build();
    }

    private static RoutineGroupResponseDTO.RoutineItem toRoutineItem(Routine routine, RoutineExecution execution) {
        boolean isCompleted = execution != null && Boolean.TRUE.equals(execution.getIsCompleted());
        Integer completedTimeSec = execution == null ? null : execution.getDurationSecond();

        return RoutineGroupResponseDTO.RoutineItem.builder()
                .routineId(routine.getId())
                .title(routine.getTitle())
                .isCompleted(isCompleted)
                .completedTimeSec(completedTimeSec)
                .build();
    }
}