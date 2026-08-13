package com.moru.server.domain.routine.service.command.Routine;

import com.moru.server.domain.routine.converter.RoutineConverter;
import com.moru.server.domain.routine.dto.RoutineResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.repository.RoutineRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.idempotency.DeletedResourceTombstoneService;
import com.moru.server.global.idempotency.IdempotencyService;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineCommandServiceImpl implements RoutineCommandService {

    private final RoutineRepository routineRepository;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;
    private final DeletedResourceTombstoneService tombstoneService;

    @Override
    public RoutineResponseDTO.DeleteResponse deleteRoutine(
            Long memberId,
            Long routineId,
            String idempotencyKey
    ) {
        return idempotencyService.execute(
                "delete-routine:" + routineId,
                memberId,
                idempotencyKey,
                routineId,
                RoutineResponseDTO.DeleteResponse.class,
                () -> transactionTemplate.execute(status -> {
                    Routine routine = routineRepository.findById(routineId).orElse(null);

                    if (routine == null) {
                        if (tombstoneService.wasDeletedBy("routine", routineId, memberId)) {
                            return RoutineConverter.toDeleteResponse(routineId);
                        }
                        throw new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND);
                    }

                    if (!routine.getRoutineGroup().isOwnedBy(memberId)) {
                        throw new BusinessException(ErrorStatus.ROUTINE_GROUP_FORBIDDEN);
                    }

                    routineRepository.delete(routine);
                    markDeletedAfterCommit("routine", routineId, memberId);   // 변경

                    return RoutineConverter.toDeleteResponse(routineId);
                })
        );
    }

    private void markDeletedAfterCommit(String resourceType, Long resourceId, Long ownerId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            tryMarkDeleted(resourceType, resourceId, ownerId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tryMarkDeleted(resourceType, resourceId, ownerId);
            }
        });
    }

    private void tryMarkDeleted(String resourceType, Long resourceId, Long ownerId) {
        try {
            tombstoneService.markDeleted(resourceType, resourceId, ownerId);
        } catch (Exception e) {
            log.warn("[Tombstone] 삭제 기록 저장 실패 (best-effort). type={}, id={}", resourceType, resourceId, e);
        }
    }
}
