package com.moru.server.domain.routine.service.command.AI;

import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutineCommandAiServiceImpl implements RoutineCommandAiService {

    private final RoutineGroupAiGenerator routineGroupAiGenerator;

    @Override
    public RoutineGroupAiGenerateResponseDTO generateRoutineGroup(String userInput) {
        return routineGroupAiGenerator.generate(userInput);
    }
}
