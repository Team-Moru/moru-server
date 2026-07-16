package com.moru.server.domain.routine.service.query.RoutineGroup;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.moru.server.domain.routine.converter.RoutineGroupConverter;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.RoutineExecution;
import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.repository.RoutineExecutionRepository;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineGroupQueryServiceImpl implements RoutineGroupQueryService {

    private final RoutineGroupRepository routineGroupRepository;
    private final RoutineExecutionRepository routineExecutionRepository;

    //루틴 그룹 상세 조회
    @Override
    public RoutineGroupResponseDTO.DetailResponse getRoutineGroupDetail(Long memberId, Long routineGroupId) {
        RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                .filter(rg -> rg.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));
        return RoutineGroupConverter.toDetailResponse(routineGroup);
    }

    // 루틴 그룹 목록 조회
    @Override
    public List<RoutineGroupResponseDTO.SummaryResponse> getRoutineGroups(Long memberId) {
        List<RoutineGroup> routineGroups = routineGroupRepository.findAllWithRoutinesByMemberId(memberId);
        return routineGroups.stream()
                .map(RoutineGroupConverter::toSummaryResponse)
                .toList();
    }

    // 오늘의 루틴 진행 현황 조회
    @Override
    public RoutineGroupResponseDTO.TodayResponse getTodayRoutine(Long memberId) {
        RoutineGroup routineGroup = routineGroupRepository
                .findFirstByMember_IdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ACTIVE_ROUTINE_GROUP_NOT_FOUND));

        int totalCount = routineGroup.getRoutineCount();
        int completedCount = routineExecutionRepository.countCompletedByRoutineGroupIdAndExecutedDate(
                routineGroup.getId(), LocalDate.now());

        return RoutineGroupConverter.toTodayResponse(completedCount, totalCount);
    }

    // 사용 중인 루틴 그룹 조회
    @Override
    public RoutineGroupResponseDTO.ActiveRoutineResponse getActiveRoutine(Long memberId) {
        RoutineGroup routineGroup = routineGroupRepository
                .findFirstByMember_IdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ACTIVE_ROUTINE_GROUP_NOT_FOUND));

        List<RoutineExecution> executions = routineExecutionRepository
                .findByRoutineGroupIdAndExecutedDate(routineGroup.getId(), LocalDate.now());
        Map<Long, RoutineExecution> executionByRoutineId = executions.stream()
                .collect(Collectors.toMap(execution -> execution.getRoutine().getId(), Function.identity()));

        return RoutineGroupConverter.toActiveRoutineResponse(routineGroup, executionByRoutineId);
    }
}
