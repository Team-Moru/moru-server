package com.moru.server.domain.routine.service.query.RoutineGroup;

import java.util.List;

import com.moru.server.domain.routine.converter.RoutineGroupConverter;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.RoutineGroup;
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
}
