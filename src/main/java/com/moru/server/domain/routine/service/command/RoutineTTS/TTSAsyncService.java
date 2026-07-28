package com.moru.server.domain.routine.service.command.RoutineTTS;

import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.event.RoutineTtsCreatedEvent;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;
import com.moru.server.global.ai.AiClient;
import com.moru.server.global.ai.dto.GeminiResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * 작업 흐름 제어 <b>서비스</b>. 합성(GoogleTtsClient) → 업로드(S3Uploader) →
 * 상태기록(RoutineTTS)의 순서만 조율한다. 실제 외부 호출은 두 전용 컴포넌트가 한다.
 *
 * <p><b>이 클래스가 따로 존재하는 이유가 핵심이다.</b>
 * Spring 의 @Async 는 프록시(대리 객체)로 동작한다. 다른 빈이 이 클래스의 메서드를
 * 호출하면 프록시가 가로채서 "별도 스레드로 돌려라" 를 수행한다. 그런데 같은 클래스
 * 안에서 자기 메서드를 부르면(self-invocation) 프록시를 거치지 않아 @Async 가 조용히
 * 무시되고 동기 실행된다. 그래서 호출하는 쪽과 이 클래스를 반드시 분리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TTSAsyncService {

    private final GoogleTtsClient googleTtsClient;
    private final S3Uploader s3Uploader;
    private final RoutineTTSRepository routineTTSRepository;
    private final AiClient aiClient;
    private static final String CONTENT_TYPE = "audio/mpeg";

    /**
     * 호출 즉시 반환되고, 실제 작업은 ttsExecutor 스레드가 이어서 한다.
     *
     * <p>반환 타입이 void 인 점이 중요하다. @Async + void 조합에서는 메서드가 던진
     * 예외를 아무도 받지 못한다(호출한 쪽은 이미 응답을 보내고 떠났다). 그래서 예외를
     * 밖으로 던지지 않고 여기서 잡아 FAILED 로 기록한다 → 조회 API 로 실패를 알 수 있다.
     *
     * <p>호출부가 직접 부르지 않고, 커밋 완료 후 Spring 이 대신 부른다.
     * 두 어노테이션이 각각 다른 걸 보장하므로 <b>둘 다</b> 필요하다.
     * <ul>
     *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)} — <b>언제</b>: 커밋된 뒤에만.
     *       이게 없으면 커밋 전에 출발해 findById 가 empty 가 되고, 그 행은 FAILED 기록도
     *       없이 영원히 PENDING 으로 남는다.</li>
     *   <li>{@code @Async} — <b>어느 스레드에서</b>: ttsExecutor 풀에서.
     *       Spring 이벤트는 기본이 <b>동기</b>라 이게 없으면 커밋한 스레드가 그대로 실행한다.</li>
     * </ul>
     *
     * @param event 저장된 routine_tts 의 id
     */

    @Async("ttsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void synthesizeAndUpload(RoutineTtsCreatedEvent event) {

        Long routineTtsId = event.routineTtsId();

        log.info("[TTS] 백그라운드 작업 시작. routineTtsId={}, thread={}",
                routineTtsId, Thread.currentThread().getName());

        String sourceText = routineTTSRepository.findById(routineTtsId)
                .map(RoutineTTS::getContent)
                .orElse(null);

        if (sourceText == null) {
            log.warn("[TTS] 대상 행이 없어 중단. routineTtsId={}", routineTtsId);
            return;
        }

        try {

            GeminiResponseDTO.AiTtsResult ttsScript = aiClient.generateTtsScript(sourceText);
            byte[] audio = googleTtsClient.synthesize(ttsScript.ttsIntro());

            String key = "tts/%d-%s.mp3".formatted(routineTtsId, UUID.randomUUID());
            s3Uploader.upload(key, audio, CONTENT_TYPE);

            RoutineTTS entity = routineTTSRepository.findById(routineTtsId)
                    .orElseThrow(() -> new IllegalStateException("RoutineTTS 행 없음: " + routineTtsId));

            entity.markCompleted(key, ttsScript.ttsIntro(), ttsScript.ttsDone());
            routineTTSRepository.save(entity);

            log.info("[TTS] 완료. routineTtsId={}, key={}, {} bytes", routineTtsId, key, audio.length);

        } catch (Exception e) {

            log.error("[TTS] 실패. routineTtsId={}", routineTtsId, e);
            markFailedQuietly(routineTtsId);
        }
    }

    private void markFailedQuietly(Long routineTtsId) {
        try {
            routineTTSRepository.findById(routineTtsId).ifPresent(entity -> {
                entity.markFailed();
                routineTTSRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("[TTS] 실패 상태 기록조차 실패. routineTtsId={}", routineTtsId, e);
        }
    }
}
