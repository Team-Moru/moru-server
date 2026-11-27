package com.moru.server.domain.routine.service.command.AI;

import java.util.UUID;

import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;
import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.redis.UsageQuotaService;
import com.moru.server.global.redis.UsageQuotaService.Limits;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutineCommandAiServiceImpl implements RoutineCommandAiService {

    private final RoutineGroupAiGenerator routineGroupAiGenerator;
    private final UsageQuotaService quota;
    private final V2UsageProperties properties;

    @Override
    public RoutineGroupAiGenerateResponseDTO generateRoutineGroup(String userInput, UUID installationId) {
        quota.consume("ai", installationId == null ? null : installationId.toString(),
                installationId == null ? null : new Limits(properties.getAiPerMinute(), properties.getAiPerDay()),
                new Limits(properties.getAiGlobalPerMinute(), properties.getAiGlobalPerDay()),
                ErrorStatus.AI_ROUTINE_QUOTA_EXCEEDED);
        return routineGroupAiGenerator.generate(userInput);
    }
}
