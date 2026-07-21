package com.moru.server.domain.routine.service.command.AI;

import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;

public interface RoutineCommandAiService {
    RoutineGroupAiGenerateResponseDTO generateRoutineGroup(String userInput);
}
