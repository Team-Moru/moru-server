package com.moru.server.domain.routine.service.command.RoutineTTS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.member.service.MemberWithdrawalLock;
import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.entity.enums.TtsStatus;
import com.moru.server.domain.routine.event.RoutineTtsCreatedEvent;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;
import com.moru.server.global.ai.AiClient;

@ExtendWith(MockitoExtension.class)
class TTSAsyncServiceTest {

    @Mock
    private GoogleTtsClient googleTtsClient;

    @Mock
    private S3Uploader s3Uploader;

    @Mock
    private RoutineTTSRepository routineTTSRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private MemberWithdrawalLock memberWithdrawalLock;

    private TTSAsyncService ttsAsyncService;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        ttsAsyncService = new TTSAsyncService(
                googleTtsClient,
                s3Uploader,
                routineTTSRepository,
                aiClient,
                memberWithdrawalLock,
                directExecutor,
                directExecutor
        );
    }

    @Test
    void marksTtsFailedWhenWithdrawalLockLookupFails() {
        RoutineTTS routineTTS = RoutineTTS.builder()
                .id(1L)
                .content("루틴 시작")
                .orderIndex(1)
                .build();
        when(routineTTSRepository.findById(1L)).thenReturn(Optional.of(routineTTS));
        when(routineTTSRepository.findMemberIdByRoutineTtsId(1L)).thenReturn(10L);
        when(memberWithdrawalLock.isLocked(10L))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        ttsAsyncService.onRoutineTtsCreated(new RoutineTtsCreatedEvent(1L, "voice-name", 0L));

        assertThat(routineTTS.getTtsStatus()).isEqualTo(TtsStatus.FAILED);
        verify(routineTTSRepository).save(routineTTS);
    }
}
