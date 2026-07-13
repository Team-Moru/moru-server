package com.moru.server.domain.routine.service.command.RoutineExecution;


import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.routine.converter.RoutineExecutionConverter;
import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;
import com.moru.server.domain.routine.repository.RoutineExecutionRepository;
import com.moru.server.domain.routine.repository.RoutineRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RoutineExecutionCommandServiceImpl implements RoutineExecutionCommandService {

    private final RoutineExecutionRepository routineExecutionRepository;
    private final RoutineRepository routineRepository;



    @Override
    public RoutineExecutionResponseDTO.RoutineExecutionResultRes saveExecutionResult(Long memberId, RoutineExecutionRequestDTO.RoutineExecutionResultReq req) {

        Routine routine = routineRepository.findById(req.routineId())
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));


        if(!routine.getRoutineGroup().getMember().getId().equals(memberId)){
            throw new BusinessException(ErrorStatus.ROUTINE_FORBIDDEN);
        }

        RoutineExecution routineExecution = RoutineExecutionConverter.toEntity(req,routine);
        routineExecutionRepository.save(routineExecution);


        return RoutineExecutionConverter.toResponse(routineExecution);
    }
}
