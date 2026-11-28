package com.moru.server.domain.routine.service.command.AI;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.exception.QuotaExceededException;
import com.moru.server.global.redis.UsageQuotaService;
import com.moru.server.global.redis.UsageQuotaService.Limits;
import com.moru.server.global.response.code.status.ErrorStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RoutineCommandAiServiceImplTest {
    @Test
    void doesNotCallGeminiWhenTheInstallationQuotaIsExceeded() {
        var id = UUID.randomUUID();
        var generator = mock(RoutineGroupAiGenerator.class);
        var quota = mock(UsageQuotaService.class);
        var properties = new V2UsageProperties();
        var service = new RoutineCommandAiServiceImpl(generator, quota, properties);
        doThrow(new QuotaExceededException(ErrorStatus.AI_ROUTINE_QUOTA_EXCEEDED, 12))
                .when(quota).consume("ai", id.toString(), new Limits(2, 5), new Limits(10, 300),
                        ErrorStatus.AI_ROUTINE_QUOTA_EXCEEDED);

        assertThatThrownBy(() -> service.generateRoutineGroup("물 마시기", id))
                .isInstanceOf(QuotaExceededException.class);
        verifyNoInteractions(generator);
    }
}
