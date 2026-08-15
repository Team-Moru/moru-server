package com.moru.server.domain.routine.service.command.RoutineTTS;

import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.event.RoutineTtsCreatedEvent;
import com.moru.server.domain.routine.event.RoutineTtsVoiceChangedEvent;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;
import com.moru.server.domain.member.service.MemberWithdrawalLock;
import com.moru.server.global.ai.AiClient;
import com.moru.server.global.ai.dto.GeminiResponseDTO;
import com.moru.server.global.logging.SanitizedLogException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
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
    private final Executor ttsRegenerateExecutor;
    private final MemberWithdrawalLock memberWithdrawalLock;
    private static final String CONTENT_TYPE = "audio/mpeg";

    public TTSAsyncService(
            GoogleTtsClient googleTtsClient,
            S3Uploader s3Uploader,
            RoutineTTSRepository routineTTSRepository,
            AiClient aiClient,
            MemberWithdrawalLock memberWithdrawalLock,
            @Qualifier("ttsExecutor") Executor ttsExecutor,
            @Qualifier("ttsRegenerateExecutor") Executor ttsRegenerateExecutor
    ) {
        this.googleTtsClient = googleTtsClient;
        this.s3Uploader = s3Uploader;
        this.routineTTSRepository = routineTTSRepository;
        this.aiClient = aiClient;
        this.memberWithdrawalLock = memberWithdrawalLock;
        this.ttsExecutor = ttsExecutor;
        this.ttsRegenerateExecutor = ttsRegenerateExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoutineTtsCreated(RoutineTtsCreatedEvent event) {
        Long routineTtsId = event.routineTtsId();
        String voiceName = event.voiceName();
        Long voiceVersion = event.voiceVersion();
        try {
            ttsExecutor.execute(() -> synthesizeAndUpload(routineTtsId, voiceName, voiceVersion));
        } catch (RejectedExecutionException e) {
            log.error("[TTS] 스레드풀 포화로 작업이 거절됨. routineTtsId={}, exceptionType={}",
                    routineTtsId,
                    e.getClass().getSimpleName(),
                    SanitizedLogException.from(e));
            markFailedQuietly(routineTtsId);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoutineTtsVoiceChanged(RoutineTtsVoiceChangedEvent event) {
        Long routineTtsId = event.routineTtsId();
        String voiceName = event.voiceName();
        Long voiceVersion = event.voiceVersion();
        try {
            ttsRegenerateExecutor.execute(() -> regenerate(routineTtsId, voiceName, voiceVersion));
        } catch (RejectedExecutionException e) {
            log.error("[TTS] 재합성 스레드풀 포화로 작업이 거절됨. routineTtsId={}, exceptionType={}",
                    routineTtsId,
                    e.getClass().getSimpleName(),
                    SanitizedLogException.from(e));
            markFailedQuietly(routineTtsId);
        }
    }

    private void synthesizeAndUpload(Long routineTtsId, String voiceName, Long voiceVersion) {

        log.info("[TTS] 백그라운드 작업 시작. routineTtsId={}", routineTtsId);

        String uploadedKey = null;

        try {
            String sourceText = routineTTSRepository.findById(routineTtsId)
                    .map(RoutineTTS::getContent)
                    .orElse(null);
            Long memberId = routineTTSRepository.findMemberIdByRoutineTtsId(routineTtsId);

            if (sourceText == null || memberId == null) {
                log.warn("[TTS] 대상 행이 없어 중단. routineTtsId={}", routineTtsId);
                return;
            }
            if (memberWithdrawalLock.isLocked(memberId)) {
                log.info("[TTS] 회원탈퇴 처리 중이므로 작업을 중단함. routineTtsId={}", routineTtsId);
                markFailedQuietly(routineTtsId);
                return;
            }

            GeminiResponseDTO.AiTtsResult ttsScript = aiClient.generateTtsScript(sourceText);
            byte[] audio = googleTtsClient.synthesize(ttsScript.ttsIntro(), voiceName);

            if (memberWithdrawalLock.isLocked(memberId)) {
                markFailedQuietly(routineTtsId);
                return;
            }

            String key = "tts/members/%d/%d-%s.mp3"
                    .formatted(memberId, routineTtsId, UUID.randomUUID());
            s3Uploader.upload(key, audio, CONTENT_TYPE);
            uploadedKey = key;

            if (memberWithdrawalLock.isLocked(memberId)) {
                deleteQuietly(uploadedKey, routineTtsId);
                markFailedQuietly(routineTtsId);
                return;
            }

            RoutineTTS entity = routineTTSRepository.findById(routineTtsId)
                    .orElseThrow(() -> new IllegalStateException("RoutineTTS 행이 없습니다."));

            entity.markCompleted(key, ttsScript.ttsIntro(), ttsScript.ttsDone(), voiceVersion);
            routineTTSRepository.save(entity);

            log.info("[TTS] 생성 및 업로드 완료. routineTtsId={}", routineTtsId);

        } catch (Exception e) {

            log.error("[TTS] 생성 또는 업로드 실패. routineTtsId={}, exceptionType={}",
                    routineTtsId,
                    e.getClass().getSimpleName(),
                    SanitizedLogException.from(e));
            deleteQuietly(uploadedKey, routineTtsId);
            markFailedQuietly(routineTtsId);
        }
    }

    private void regenerate(Long routineTtsId, String voiceName, Long voiceVersion) {

        log.info("[TTS] 재합성 시작. routineTtsId={}, voiceVersion={}", routineTtsId, voiceVersion);

        String previousKey = null;
        String uploadedKey = null;

        try {
            // DB·Redis 조회도 실패할 수 있다. try 밖에 두면 예외가 executor 로 빠져나가
            // markFailedQuietly 가 실행되지 않고, 행이 PENDING 에 갇힌다.
            RoutineTTS entity = routineTTSRepository.findById(routineTtsId).orElse(null);
            Long memberId = routineTTSRepository.findMemberIdByRoutineTtsId(routineTtsId);

            if (entity == null || memberId == null) {
                log.warn("[TTS] 재합성 대상 행이 없어 중단. routineTtsId={}", routineTtsId);
                return;
            }
            if (memberWithdrawalLock.isLocked(memberId)) {
                log.info("[TTS] 회원탈퇴 처리 중이므로 재합성을 중단함. routineTtsId={}", routineTtsId);
                return;
            }

            previousKey = entity.getS3Url();

            String intro = entity.getTtsIntro();
            String done = entity.getTtsDone();

            // 최초 합성이 끝난 적 없는 행은 재사용할 멘트가 없다. 이때만 생성 경로와 동일하게 만든다.
            if (intro == null || intro.isBlank()) {
                GeminiResponseDTO.AiTtsResult ttsScript = aiClient.generateTtsScript(entity.getContent());
                intro = ttsScript.ttsIntro();
                done = ttsScript.ttsDone();
            }

            byte[] audio = googleTtsClient.synthesize(intro, voiceName);

            if (memberWithdrawalLock.isLocked(memberId)) {
                return;
            }

            String key = "tts/members/%d/%d-%s.mp3"
                    .formatted(memberId, routineTtsId, UUID.randomUUID());
            s3Uploader.upload(key, audio, CONTENT_TYPE);
            uploadedKey = key;

            if (memberWithdrawalLock.isLocked(memberId)) {
                deleteQuietly(uploadedKey, routineTtsId);
                return;
            }

            Long currentVersion = routineTTSRepository.findCurrentVoiceVersion(routineTtsId).orElse(null);
            if (!Objects.equals(currentVersion, voiceVersion)) {
                log.info("[TTS] 재합성 중 목소리가 다시 바뀌어 결과를 폐기. routineTtsId={}, 작업버전={}, 현재버전={}",
                        routineTtsId, voiceVersion, currentVersion);
                deleteQuietly(uploadedKey, routineTtsId);
                return;
            }

            RoutineTTS target = routineTTSRepository.findById(routineTtsId)
                    .orElseThrow(() -> new IllegalStateException("RoutineTTS 행이 없습니다."));

            target.markCompleted(key, intro, done, voiceVersion);
            routineTTSRepository.save(target);

            // 교체가 확정된 뒤에야 옛 음원을 지운다. 먼저 지우면 실패 시 들려줄 음원이 사라진다.
            if (previousKey != null && !previousKey.equals(key)) {
                deleteQuietly(previousKey, routineTtsId);
            }

            log.info("[TTS] 재합성 완료. routineTtsId={}", routineTtsId);

        } catch (Exception e) {

            log.error("[TTS] 재합성 실패. 기존 음원은 유지한다. routineTtsId={}, exceptionType={}",
                    routineTtsId,
                    e.getClass().getSimpleName(),
                    SanitizedLogException.from(e));
            deleteQuietly(uploadedKey, routineTtsId);
            markFailedQuietly(routineTtsId);
        }
    }

    private void deleteQuietly(String key, Long routineTtsId) {
        if (key == null) {
            return;
        }
        try {
            s3Uploader.delete(key);
            log.info("[TTS] 사용되지 않는 객체를 삭제함. routineTtsId={}", routineTtsId);
        } catch (Exception e) {
            log.error("[TTS] 객체 삭제 실패. routineTtsId={}, exceptionType={}",
                    routineTtsId,
                    e.getClass().getSimpleName(),
                    SanitizedLogException.from(e));
        }
    }

    private void markFailedQuietly(Long routineTtsId) {
        try {
            routineTTSRepository.findById(routineTtsId).ifPresent(entity -> {
                entity.markFailed();
                routineTTSRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("[TTS] 실패 상태 기록조차 실패. routineTtsId={}, exceptionType={}",
                    routineTtsId,
                    e.getClass().getSimpleName(),
                    SanitizedLogException.from(e));
        }
    }
}
