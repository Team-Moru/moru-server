package com.moru.server.domain.routine.service.query.RoutineGroup;

import com.moru.server.domain.routine.converter.RoutineGroupConverter;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineGroupQueryServiceImpl implements RoutineGroupQueryService {

    private final RoutineGroupRepository routineGroupRepository;

    // 루틴 그룹 조회
    @Override
    public List<RoutineGroupResponseDTO.SummaryResponse> getRoutineGroups(Long memberId) {
        List<RoutineGroup> routineGroups = routineGroupRepository.findAllWithRoutinesByMemberId(memberId);

        return routineGroups.stream()
                .map(RoutineGroupConverter::toSummaryResponse)
                .toList();
    }
}
