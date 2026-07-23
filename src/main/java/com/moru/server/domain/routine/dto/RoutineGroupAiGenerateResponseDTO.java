package com.moru.server.domain.routine.dto;

import com.moru.server.domain.routine.entity.enums.RoutineType;

import java.util.List;

public record RoutineGroupAiGenerateResponseDTO(
        String title,
        String description,
        List<RoutineDTO> routines
) {
    public record RoutineDTO(
            String title,
            RoutineType type,
            int durationSecond
    ) {}
}