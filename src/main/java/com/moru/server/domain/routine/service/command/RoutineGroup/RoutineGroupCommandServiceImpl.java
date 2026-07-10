package com.moru.server.domain.routine.service.command.RoutineGroup;
import java.util.ArrayList;
import java.util.List;

import com.moru.server.domain.routine.converter.RoutineGroupConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.routine.dto.RoutineGroupRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineGroupCommandServiceImpl implements RoutineGroupCommandService {

    private final RoutineGroupRepository routineGroupRepository;
    private final MemberRepository memberRepository;

    // 루틴 그룹 생성
    @Override
    public RoutineGroupResponseDTO.CreateResponse createRoutineGroup(
            Long memberId,
            RoutineGroupRequestDTO.CreateRequest request
    ) {
        if (request.routines() == null || request.routines().isEmpty()) {
            throw new BusinessException(ErrorStatus.ROUTINE_EMPTY);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        RoutineGroup routineGroup = RoutineGroup.builder()
                .title(request.title())
                .description(request.description())
                .alarmDays(request.alarmDays())
                .alarmTime(request.alarmTime())
                .weatherNotificationEnabled(request.weatherNotificationEnabled())
                .member(member)
                .build();

        List<Routine> routines = createRoutines(request.routines(), routineGroup);
        routineGroup.getRoutines().addAll(routines);

        RoutineGroup savedRoutineGroup = routineGroupRepository.save(routineGroup);

        return RoutineGroupConverter.toCreateResponse(savedRoutineGroup);
    }

    private List<Routine> createRoutines(
            List<RoutineGroupRequestDTO.RoutineRequest> routineRequests,
            RoutineGroup routineGroup
    ) {
        List<Routine> routines = new ArrayList<>();
        for (int i = 0; i < routineRequests.size(); i++) {
            RoutineGroupRequestDTO.RoutineRequest routineRequest = routineRequests.get(i);
            routines.add(Routine.builder()
                    .title(routineRequest.title())
                    .type(routineRequest.type())
                    .timer(routineRequest.durationSecond())
                    .orderIndex(i)
                    .routineGroup(routineGroup)
                    .build());
        }
        return routines;
    }

    //루틴 그룹 삭제
    @Override
    public RoutineGroupResponseDTO.DeleteResponse deleteRoutineGroup(Long memberId, Long routineGroupId) {
        RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        if (!routineGroup.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND);
        }

        routineGroupRepository.delete(routineGroup);

        return RoutineGroupConverter.toDeleteResponse(routineGroupId);
    }
  
    // 루틴 그룹 토글 (활성/비활성)
    @Override
    public RoutineGroupResponseDTO.ActiveResponse updateActive(
            Long memberId,
            Long routineGroupId,
            RoutineGroupRequestDTO.ActiveRequest request
    ) {
        RoutineGroup routineGroup = routineGroupRepository.findById(routineGroupId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        if (!routineGroup.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND);
        }

        routineGroup.updateActive(request.isActive());

        return RoutineGroupConverter.toActiveResponse(routineGroup);
    }
}