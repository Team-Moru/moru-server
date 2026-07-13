package com.moru.server.domain.routine.service.command.Routine;

import com.moru.server.domain.routine.converter.RoutineConverter;
import com.moru.server.domain.routine.dto.RoutineResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.repository.RoutineRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineCommandServiceImpl implements RoutineCommandService {

    private final RoutineRepository routineRepository;

    @Override
    public RoutineResponseDTO.DeleteResponse deleteRoutine(Long memberId, Long routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));

        if (!routine.getRoutineGroup().isOwnedBy(memberId)) {
            throw new BusinessException(ErrorStatus.ROUTINE_GROUP_FORBIDDEN);
        }

        routineRepository.delete(routine);

        return RoutineConverter.toDeleteResponse(routineId);
    }
}
