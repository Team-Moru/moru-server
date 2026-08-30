package com.moru.server.domain.routine.service.command.RoutineExecution;



import com.moru.server.domain.routine.converter.RoutineExecutionConverter;
import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;
import com.moru.server.domain.routine.repository.RoutineExecutionRepository;
import com.moru.server.domain.routine.repository.RoutineRepository;
import com.moru.server.global.ai.AiClient;
import com.moru.server.global.ai.dto.GeminiResponseDTO;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.idempotency.IdempotencyService;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RoutineExecutionCommandServiceImpl implements RoutineExecutionCommandService {

    private final RoutineExecutionRepository routineExecutionRepository;
    private final RoutineRepository routineRepository;
    private final AiClient aiClient;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;


    @Override
    public RoutineExecutionResponseDTO.RoutineExecutionResultRes saveExecutionResult(
            Long memberId,
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req,
            String idempotencyKey
    ) {
        return idempotencyService.execute(
                "save-execution-result:" + req.routineId(),
                memberId,
                idempotencyKey,
                req,
                RoutineExecutionResponseDTO.RoutineExecutionResultRes.class,
                () -> doSaveExecutionResult(memberId, req)
        );
    }

    private RoutineExecutionResponseDTO.RoutineExecutionResultRes doSaveExecutionResult(
            Long memberId,
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req
    ) {
        try {
            return transactionTemplate.execute(status -> {
                Routine routine = routineRepository.findById(req.routineId())
                        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));

                if (!routine.getRoutineGroup().isOwnedBy(memberId)) {
                    throw new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND);
                }

                RoutineExecution routineExecution = findOrCreateExecution(
                        req.routineId(), req.executedDate(), routine, req
                );
                return RoutineExecutionConverter.toResponse(routineExecution);
            });
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 다른 트랜잭션이 먼저 insert함 -> 새 트랜잭션에서 재조회 후 업데이트
            return transactionTemplate.execute(status -> {
                RoutineExecution raceWinner = routineExecutionRepository
                        .findByRoutine_IdAndExecutedDate(req.routineId(), req.executedDate())
                        .orElseThrow(() -> e);
                RoutineExecution updated = applyExecutionResult(raceWinner, req);
                return RoutineExecutionConverter.toResponse(updated);
            });
        }
    }

    // try-catch 없이 "찾거나 새로 만들거나"만 함 - 예외는 그대로 위로 던져서 트랜잭션이 자연스럽게 롤백
    private RoutineExecution findOrCreateExecution(
            Long routineId,
            LocalDate executedDate,
            Routine routine,
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req
    ) {
        return routineExecutionRepository
                .findByRoutine_IdAndExecutedDate(routineId, executedDate)
                .map(existing -> applyExecutionResult(existing, req))
                .orElseGet(() -> routineExecutionRepository.saveAndFlush(
                        RoutineExecutionConverter.toEntity(req, routine)
                ));
    }


    private RoutineExecution applyExecutionResult(
            RoutineExecution existing,
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req
    ) {
        if (Boolean.TRUE.equals(req.isCompleted())) {
            existing.complete(req.durationSecond());
        } else {
            existing.fail(req.durationSecond());
        }
        if (req.memberInput() != null) {
            existing.recordInput(req.memberInput());
        }
        if (req.aiResponse() != null) {
            existing.recordAiResponse(req.aiResponse());
        }
        if (req.actualWakeTime() != null) {
            existing.recordActualWakeTime(req.actualWakeTime());
        }
        return existing;
    }


    @Override
    public RoutineExecutionResponseDTO.AiResponseRes judgeUserResponse(Long memberId, RoutineExecutionRequestDTO.AiResponseReq req) {
        return idempotencyService.execute(
                "judge-user-response",
                memberId,
                req.clientExecutionId(),
                req,
                RoutineExecutionResponseDTO.AiResponseRes.class,
                () -> doJudgeUserResponse(memberId, req)
        );
    }

    private RoutineExecutionResponseDTO.AiResponseRes doJudgeUserResponse(
            Long memberId,
            RoutineExecutionRequestDTO.AiResponseReq req
    ) {
        Routine routine = routineRepository.findWithGroupById(req.routineId())
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));

        if (!routine.getRoutineGroup().isOwnedBy(memberId)) {
            throw new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND);
        }

        GeminiResponseDTO.AiJudgeResult dto = aiClient.judge(req.memberInput());

        if (dto.failed()) {
            throw new BusinessException(ErrorStatus.AI_JUDGE_FAILED);
        }

        if (dto.shouldProceed()) {
            saveJudgeResultWithRetry(req, routine, dto.aiResponse());
        }

        return RoutineExecutionResponseDTO.AiResponseRes.builder()
                .aiResponse(dto.aiResponse())
                .shouldProceed(dto.shouldProceed())
                .build();
    }

    private void saveJudgeResultWithRetry(
            RoutineExecutionRequestDTO.AiResponseReq req,
            Routine routine,
            String aiResponse
    ) {
        try {
            transactionTemplate.execute(status -> {
                findOrCreateJudgeResult(req.routineId(), req.executedDate(), routine, req, aiResponse);
                return null;
            });
        } catch (DataIntegrityViolationException e) {
            transactionTemplate.execute(status -> {
                RoutineExecution raceWinner = routineExecutionRepository
                        .findByRoutine_IdAndExecutedDate(req.routineId(), req.executedDate())
                        .orElseThrow(() -> e);
                applyJudgeResult(raceWinner, req, aiResponse);
                return null;
            });
        }
    }

    private RoutineExecution findOrCreateJudgeResult(
            Long routineId,
            LocalDate executedDate,
            Routine routine,
            RoutineExecutionRequestDTO.AiResponseReq req,
            String aiResponse
    ) {
        return routineExecutionRepository
                .findByRoutine_IdAndExecutedDate(routineId, executedDate)
                .map(existing -> applyJudgeResult(existing, req, aiResponse))
                .orElseGet(() -> routineExecutionRepository.saveAndFlush(
                        RoutineExecutionConverter.toEntity(req, routine, aiResponse)
                ));
    }

    private RoutineExecution applyJudgeResult(
            RoutineExecution existing,
            RoutineExecutionRequestDTO.AiResponseReq req,
            String aiResponse
    ) {
        existing.complete(req.durationSecond());
        existing.recordAiResponse(aiResponse);
        if (req.memberInput() != null) {
            existing.recordInput(req.memberInput());
        }
        if (req.actualWakeTime() != null) {
            existing.recordActualWakeTime(req.actualWakeTime());
        }
        return existing;
    }

}
