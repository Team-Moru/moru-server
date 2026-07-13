package com.moru.server.domain.routine.converter;

import com.moru.server.domain.routine.dto.RoutineResponseDTO;

public class RoutineConverter {

    public static RoutineResponseDTO.DeleteResponse toDeleteResponse(Long routineId) {
        return RoutineResponseDTO.DeleteResponse.builder()
                .routineId(routineId)
                .build();
    }
}