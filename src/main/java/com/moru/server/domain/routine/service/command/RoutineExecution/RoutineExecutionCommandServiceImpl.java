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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutineExecutionCommandServiceImpl implements RoutineExecutionCommandService {

    private final RoutineExecutionRepository routineExecutionRepository;
    private final RoutineRepository routineRepository;
    private final AiClient aiClient;
    private final IdempotencyService idempotencyService;


    @Override
    @Transactional
    public RoutineExecutionResponseDTO.RoutineExecutionResultRes saveExecutionResult(
            Long memberId,
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req,
            String idempotencyKey
    ) {
        return idempotencyService.execute(
                "save-execution-result:" + req.routineId(),
                memberId,
                idempotencyKey,
                RoutineExecutionResponseDTO.RoutineExecutionResultRes.class,
                () -> {
                    Routine routine = routineRepository.findById(req.routineId())
                            .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));

                    if (!routine.getRoutineGroup().isOwnedBy(memberId)) {
                        throw new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND);
                    }

                    RoutineExecution routineExecution = RoutineExecutionConverter.toEntity(req, routine);
                    routineExecutionRepository.save(routineExecution);

                    return RoutineExecutionConverter.toResponse(routineExecution);
                }
        );
    }


    @Override
    public RoutineExecutionResponseDTO.AiResponseRes judgeUserResponse(Long memberId, RoutineExecutionRequestDTO.AiResponseReq req) {

        Routine routine = routineRepository.findWithGroupById(req.routineId())
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));


        if(!routine.getRoutineGroup().isOwnedBy(memberId)){
            throw new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND);
        }

        GeminiResponseDTO.AiJudgeResult dto = aiClient.judge(req.memberInput());

        if(dto.shouldProceed()){
            RoutineExecution routineExecution = RoutineExecutionConverter.toEntity(req,routine,dto.aiResponse());
            routineExecutionRepository.save(routineExecution);
        }


        return RoutineExecutionResponseDTO.AiResponseRes.builder()
                .aiResponse(dto.aiResponse())
                .shouldProceed(dto.shouldProceed())
                .build();
    }



}
