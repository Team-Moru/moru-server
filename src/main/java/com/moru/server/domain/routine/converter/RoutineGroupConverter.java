package com.moru.server.domain.routine.converter;


import java.util.List;

import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
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
}