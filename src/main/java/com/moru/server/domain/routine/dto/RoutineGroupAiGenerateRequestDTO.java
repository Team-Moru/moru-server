package com.moru.server.domain.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoutineGroupAiGenerateRequestDTO(
        @NotBlank
        @Size(max = 200)
        String userInput
) {}
