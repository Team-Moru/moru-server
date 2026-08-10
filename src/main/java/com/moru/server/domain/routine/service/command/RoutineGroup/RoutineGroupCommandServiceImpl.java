package com.moru.server.domain.routine.service.command.RoutineGroup;

import java.util.ArrayList;
import java.util.List;


import com.moru.server.domain.routine.converter.RoutineGroupConverter;
import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.entity.enums.RoutineType;
import com.moru.server.domain.routine.repository.RoutineRepository;
import com.moru.server.domain.routine.event.RoutineTtsCreatedEvent;
import com.moru.server.domain.routine.service.command.AI.RoutineStepGenerator;
import com.moru.server.global.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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

    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;


    @Value("${google.tts.enabled:true}")
    private boolean ttsEnabled;



    @Override
    public RoutineGroupResponseDTO.DetailResponse createRoutineGroup(
            Long memberId,
            RoutineGroupRequestDTO.CreateRequest request,
            String idempotencyKey   // 파라미터 추가됨
    ) {
        return idempotencyService.execute(
                "create-routine-group", memberId, idempotencyKey,
                request,
                RoutineGroupResponseDTO.DetailResponse.class,
                () -> doCreateRoutineGroup(memberId, request)
        );
    }

    private RoutineGroupResponseDTO.DetailResponse doCreateRoutineGroup(
            Long memberId,
            RoutineGroupRequestDTO.CreateRequest request
    ) {
        if (request.routines() == null || request.routines().isEmpty()) {
            throw new BusinessException(ErrorStatus.ROUTINE_EMPTY);
        }

        List<List<String>> stepContents = buildStepContents(request.routines());

        RoutineGroup savedRoutineGroup = transactionTemplate.execute(status -> {
            // member 조회 + 검증을 트랜잭션 안으로 이동
            Member member = memberRepository.findByIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

            validateNoAlarmDayConflict(memberId, request.alarmDays(), null);

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

        return RoutineGroupConverter.toDetailResponse(savedRoutineGroup);
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
    public RoutineGroupResponseDTO.DeleteResponse deleteRoutineGroup(
            Long memberId,
            Long routineGroupId,
            String idempotencyKey   // 파라미터 추가됨
    ) {
        return idempotencyService.execute(
                "delete-routine-group:" + routineGroupId,
                memberId,
                idempotencyKey,
                routineGroupId,
                RoutineGroupResponseDTO.DeleteResponse.class,
                () -> transactionTemplate.execute(status -> {
                    RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                            .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

                    if (!routineGroup.isOwnedBy(memberId)) {
                        throw new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND);
                    }

                    routineGroupRepository.delete(routineGroup);

                    return RoutineGroupConverter.toDeleteResponse(routineGroupId);
                })
        );
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
            RoutineGroupRequestDTO.RoutineRequest request,
            String idempotencyKey
    ) {
        return idempotencyService.execute(
                "add-routine:" + routineGroupId,
                memberId,
                idempotencyKey,
                request,
                RoutineGroupResponseDTO.RoutineResponse.class,
                () -> doAddRoutine(memberId, routineGroupId, request)
        );
    }

    // 이하 doAddRoutine으로 이름만 바꾸고, try-catch(dedup) 부분만 제거
    private RoutineGroupResponseDTO.RoutineResponse doAddRoutine(
            Long memberId,
            Long routineGroupId,
            RoutineGroupRequestDTO.RoutineRequest request
    ) {
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

        return RoutineGroupConverter.toRoutineResponse(savedRoutine);
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
