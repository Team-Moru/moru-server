package com.moru.server.domain.routine.service.command.AI;

import java.util.UUID;

import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;

public interface RoutineCommandAiService {
    RoutineGroupAiGenerateResponseDTO generateRoutineGroup(String userInput, UUID installationId);
}
