package com.moru.server.domain.routine.service.command.RoutineTTS;

import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.event.RoutineTtsCreatedEvent;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;
import com.moru.server.global.ai.AiClient;
import com.moru.server.global.ai.dto.GeminiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
@ConditionalOnProperty(name = "google.tts.enabled", havingValue = "true", matchIfMissing = false)
public class TTSAsyncService {

    private final GoogleTtsClient googleTtsClient;
    private final S3Uploader s3Uploader;
    private final RoutineTTSRepository routineTTSRepository;
    private final AiClient aiClient;
    private final Executor ttsExecutor;
    private static final String CONTENT_TYPE = "audio/mpeg";

    public TTSAsyncService(
            GoogleTtsClient googleTtsClient,
            S3Uploader s3Uploader,
            RoutineTTSRepository routineTTSRepository,
            AiClient aiClient,
            @Qualifier("ttsExecutor") Executor ttsExecutor
    ) {
        this.googleTtsClient = googleTtsClient;
        this.s3Uploader = s3Uploader;
        this.routineTTSRepository = routineTTSRepository;
        this.aiClient = aiClient;
        this.ttsExecutor = ttsExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoutineTtsCreated(RoutineTtsCreatedEvent event) {
        Long routineTtsId = event.routineTtsId();
        String voiceName = event.voiceName();
        try {
            ttsExecutor.execute(() -> synthesizeAndUpload(routineTtsId,voiceName));
        } catch (RejectedExecutionException e) {
            log.error("[TTS] 스레드풀 포화로 작업이 거절됨. exceptionType={}",
                    e.getClass().getSimpleName());
            markFailedQuietly(routineTtsId);
        }
    }

    private void synthesizeAndUpload(Long routineTtsId,String voiceName) {

        log.info("[TTS] 백그라운드 작업 시작");

        String sourceText = routineTTSRepository.findById(routineTtsId)
                .map(RoutineTTS::getContent)
                .orElse(null);

        if (sourceText == null) {
            log.warn("[TTS] 대상 행이 없어 중단");
            return;
        }

        String uploadedKey = null;

        try {

            GeminiResponseDTO.AiTtsResult ttsScript = aiClient.generateTtsScript(sourceText);
            byte[] audio = googleTtsClient.synthesize(ttsScript.ttsIntro(),voiceName);

            String key = "tts/%d-%s.mp3".formatted(routineTtsId, UUID.randomUUID());
            s3Uploader.upload(key, audio, CONTENT_TYPE);
            uploadedKey = key;

            RoutineTTS entity = routineTTSRepository.findById(routineTtsId)
                    .orElseThrow(() -> new IllegalStateException("RoutineTTS 행이 없습니다."));

            entity.markCompleted(key, ttsScript.ttsIntro(), ttsScript.ttsDone());
            routineTTSRepository.save(entity);

            log.info("[TTS] 생성 및 업로드 완료");

        } catch (Exception e) {

            log.error("[TTS] 생성 또는 업로드 실패. exceptionType={}",
                    e.getClass().getSimpleName());
            deleteQuietly(uploadedKey);
            markFailedQuietly(routineTtsId);
        }
    }

    private void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            s3Uploader.delete(key);
            log.info("[TTS] 실패로 업로드된 객체를 삭제함");
        } catch (Exception e) {
            log.error("[TTS] 업로드된 객체 삭제 실패. exceptionType={}",
                    e.getClass().getSimpleName());
        }
    }

    private void markFailedQuietly(Long routineTtsId) {
        try {
            routineTTSRepository.findById(routineTtsId).ifPresent(entity -> {
                entity.markFailed();
                routineTTSRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("[TTS] 실패 상태 기록조차 실패. exceptionType={}",
                    e.getClass().getSimpleName());
        }
    }
}
