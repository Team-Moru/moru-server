package com.moru.server.domain.routine.service.command.RoutineGroup;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moru.server.domain.routine.converter.RoutineGroupConverter;
import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.entity.enums.RoutineType;
import com.moru.server.domain.routine.repository.RoutineRepository;
import com.moru.server.domain.routine.event.RoutineTtsCreatedEvent;
import com.moru.server.domain.routine.service.command.AI.RoutineStepGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.routine.dto.RoutineGroupRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineGroupCommandServiceImpl implements RoutineGroupCommandService {

    private final RoutineGroupRepository routineGroupRepository;
    private final MemberRepository memberRepository;
    private final RoutineRepository routineRepository;
    private final RoutineStepGenerator routineStepGenerator;
    private final ApplicationEventPublisher eventPublisher;

    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    @Value("${google.tts.enabled:true}")
    private boolean ttsEnabled;

    private static final Duration DEDUP_PROCESSING_TTL = Duration.ofSeconds(30);
    private static final Duration DEDUP_RETRY_WINDOW_TTL = Duration.ofSeconds(5);
    private static final String DEDUP_KEY_PREFIX = "moru:dedup:";

    private static final DefaultRedisScript<Long> COMPLETE_IF_OWNER_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then
            return 0
        end
        redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
        return 1
        """, Long.class);


    private static final DefaultRedisScript<Long> DELETE_IF_OWNER_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then
            return 0
        end
        return redis.call('DEL', KEYS[1])
        """, Long.class);

    @Override
    public RoutineGroupResponseDTO.DetailResponse createRoutineGroup(
            Long memberId,
            RoutineGroupRequestDTO.CreateRequest request
    ) {
        DedupReservation dedupKey = reserveDedupKey("create-routine-group", memberId, request);
        try {
            if (request.routines() == null || request.routines().isEmpty()) {
                throw new BusinessException(ErrorStatus.ROUTINE_EMPTY);
            }

            Member member = memberRepository.findByIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

            validateNoAlarmDayConflict(memberId, request.alarmDays(), null);

            List<List<String>> stepContents = buildStepContents(request.routines());

            RoutineGroup savedRoutineGroup = transactionTemplate.execute(status -> {
                RoutineGroup routineGroup = RoutineGroup.builder()
                        .title(request.title())
                        .description(request.description())
                        .alarmDays(request.alarmDays())
                        .alarmTime(request.alarmTime())
                        .weatherNotificationEnabled(request.weatherNotificationEnabled())
                        .member(member)
                        .build();

                List<Routine> routines = createRoutines(request.routines(), stepContents, routineGroup);
                routineGroup.getRoutines().addAll(routines);

                RoutineGroup saved = routineGroupRepository.save(routineGroup);


                for (Routine routine : saved.getRoutines()) {
                    for (RoutineTTS tts : routine.getTtsList()) {
                        publishTtsEvent(tts);
                    }
                }

                return saved;
            });

            RoutineGroupResponseDTO.DetailResponse response = RoutineGroupConverter.toDetailResponse(savedRoutineGroup);
            markDedupCompleted(dedupKey);
            return response;
        } catch (BusinessException e) {
            if (e.getBaseCode() != ErrorStatus.DUPLICATE_REQUEST) {
                releaseDedupKey(dedupKey);
            }
            throw e;
        } catch (RuntimeException e) {
            releaseDedupKey(dedupKey);
            throw e;
        }
    }

    private List<Routine> createRoutines(
            List<RoutineGroupRequestDTO.RoutineRequest> routineRequests,
            List<List<String>> stepContents,
            RoutineGroup routineGroup
    ) {
        List<Routine> routines = new ArrayList<>();
        for (int i = 0; i < routineRequests.size(); i++) {
            RoutineGroupRequestDTO.RoutineRequest routineRequest = routineRequests.get(i);
            Routine routine = Routine.builder()
                    .title(routineRequest.title())
                    .type(routineRequest.type())
                    .timer(routineRequest.durationSecond())
                    .orderIndex(i)
                    .routineGroup(routineGroup)
                    .build();
            attachSteps(routine, stepContents.get(i));
            routines.add(routine);

        }

        return routines;
    }

    private List<List<String>> buildStepContents(List<RoutineGroupRequestDTO.RoutineRequest> routines) {
        List<String> timerTitles = new ArrayList<>();
        for (RoutineGroupRequestDTO.RoutineRequest r : routines) {
            if (r.type() == RoutineType.TIMER) {
                timerTitles.add(r.title());
            }
        }

        List<List<String>> timerSteps = generateTimerSteps(timerTitles);

        List<List<String>> result = new ArrayList<>(routines.size());
        int timerCursor = 0;
        for (RoutineGroupRequestDTO.RoutineRequest r : routines) {
            if (r.type() == RoutineType.TIMER) {
                List<String> steps = (timerCursor < timerSteps.size()) ? timerSteps.get(timerCursor) : List.of();
                timerCursor++;
                result.add(fallbackIfEmpty(steps, r.title()));
            } else {
                result.add(List.of(r.title()));
            }
        }
        return result;
    }

    private List<List<String>> generateTimerSteps(List<String> timerTitles) {
        if (timerTitles.isEmpty()) {
            return List.of();
        }
        try {
            return routineStepGenerator.generateForTimer(timerTitles);
        } catch (Exception e) {
            log.warn("TIMER step 생성 실패 - 제목으로 대체합니다. titles={}", timerTitles, e);
            return List.of();
        }
    }

    private List<String> fallbackIfEmpty(List<String> steps, String title) {
        return (steps == null || steps.isEmpty()) ? List.of(title) : steps;
    }

    private void attachSteps(Routine routine, List<String> contents) {
        for (int i = 0; i < contents.size(); i++) {
            routine.addStep(contents.get(i), i);
        }
    }

    private DedupReservation reserveDedupKey(String action, Long memberId, Object request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String requestHash = DigestUtils.sha256Hex(requestJson);
            String dedupKey = DEDUP_KEY_PREFIX + action + ":" + memberId + ":" + requestHash;
            String token = UUID.randomUUID().toString();

            Boolean isFirstRequest = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, token, DEDUP_PROCESSING_TTL);

            if (Boolean.FALSE.equals(isFirstRequest)) {
                throw new BusinessException(ErrorStatus.DUPLICATE_REQUEST);
            }
            return new DedupReservation(dedupKey, token);
        } catch (JsonProcessingException e) {
            log.warn("dedup 체크용 직렬화 실패, 검증 스킵. action={}", action, e);
            return null;
        }
    }

    private void markDedupCompleted(DedupReservation reservation) {
        if (reservation == null) {
            return;
        }
        redisTemplate.execute(
                COMPLETE_IF_OWNER_SCRIPT,
                List.of(reservation.key()),
                reservation.token(),
                String.valueOf(DEDUP_RETRY_WINDOW_TTL.toMillis())
        );
    }

    private void releaseDedupKey(DedupReservation reservation) {
        if (reservation == null) {
            return;
        }
        redisTemplate.execute(
                DELETE_IF_OWNER_SCRIPT,
                List.of(reservation.key()),
                reservation.token()
        );
    }

    private record DedupReservation(String key, String token) {}

    private void publishTtsEvent(RoutineTTS tts) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "TTS 이벤트는 트랜잭션 안에서 발행해야 한다. routineTtsId=" + tts.getId());
        }
        if (!ttsEnabled) {
            log.info("[TTS] 기능이 비활성화되어 FAILED 로 종결. routineTtsId={}", tts.getId());
            tts.markFailed();
            return;
        }
        eventPublisher.publishEvent(new RoutineTtsCreatedEvent(tts.getId()));
    }


    @Override
    @Transactional
    public RoutineGroupResponseDTO.DeleteResponse deleteRoutineGroup(Long memberId, Long routineGroupId) {
        RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        if (!routineGroup.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND);
        }

        routineGroupRepository.delete(routineGroup);

        return RoutineGroupConverter.toDeleteResponse(routineGroupId);
    }


    @Override
    @Transactional
    public RoutineGroupResponseDTO.ActiveResponse updateActive(
            Long memberId,
            Long routineGroupId,
            RoutineGroupRequestDTO.ActiveRequest request
    ) {
        RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        if (!routineGroup.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(request.isActive())) {
            memberRepository.findByIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));
            validateNoAlarmDayConflict(memberId, routineGroup.getAlarmDays(), routineGroupId);
        }

        routineGroup.updateActive(request.isActive());

        return RoutineGroupConverter.toActiveResponse(routineGroup);
    }

    @Override
    public RoutineGroupResponseDTO.RoutineResponse addRoutine(
            Long memberId,
            Long routineGroupId,
            RoutineGroupRequestDTO.RoutineRequest request
    ) {
        DedupReservation dedupKey = reserveDedupKey("add-routine:" + routineGroupId, memberId, request);
        try {
            RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                    .filter(rg -> rg.isOwnedBy(memberId))
                    .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

            List<String> stepContents = buildSingleStepContents(request);

            Routine savedRoutine = transactionTemplate.execute(status -> {
                routineGroupRepository.findByIdForUpdate(routineGroupId)
                        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

                int nextOrderIndex = routineRepository.findMaxOrderIndexByRoutineGroupId(routineGroupId)
                        .map(max -> max + 1)
                        .orElse(0);

                Routine routine = Routine.builder()
                        .title(request.title())
                        .type(request.type())
                        .timer(request.durationSecond())
                        .orderIndex(nextOrderIndex)
                        .routineGroup(routineGroup)
                        .build();
                attachSteps(routine, stepContents);

                Routine saved = routineRepository.save(routine);

                for (RoutineTTS tts : saved.getTtsList()) {
                    publishTtsEvent(tts);
                }

                return saved;
            });

            RoutineGroupResponseDTO.RoutineResponse response = RoutineGroupConverter.toRoutineResponse(savedRoutine);
            markDedupCompleted(dedupKey);
            return response;
        } catch (BusinessException e) {
            if (e.getBaseCode() != ErrorStatus.DUPLICATE_REQUEST) {
                releaseDedupKey(dedupKey);
            }
            throw e;
        } catch (RuntimeException e) {
            releaseDedupKey(dedupKey);
            throw e;
        }
    }

    private void validateNoAlarmDayConflict(Long memberId, String alarmDays, Long excludeRoutineGroupId) {
        List<RoutineGroup> activeGroups = routineGroupRepository.findByMember_IdAndIsActiveTrue(memberId);
        for (RoutineGroup group : activeGroups) {
            if (excludeRoutineGroupId != null && group.getId().equals(excludeRoutineGroupId)) {
                continue;
            }
            if (group.hasOverlappingAlarmDays(alarmDays)) {
                throw new BusinessException(ErrorStatus.ROUTINE_ALARM_DAYS_CONFLICT);
            }
        }
    }
  
    private List<String> buildSingleStepContents(RoutineGroupRequestDTO.RoutineRequest request) {
        if (request.type() != RoutineType.TIMER) {
            return List.of(request.title());
        }
        List<List<String>> steps = generateTimerSteps(List.of(request.title()));
        if (!steps.isEmpty()) {
            return fallbackIfEmpty(steps.get(0), request.title());
        }
        return List.of(request.title());
    }
}
