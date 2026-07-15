package com.moru.server.domain.routine.controller;


import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.service.command.RoutineExecution.RoutineExecutionCommandService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.response.code.status.SuccessStatus;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@Tag(name = "Routine Execution", description = "루틴 실행 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routine-executions")
public class RoutineExecutionController {

    private final RoutineExecutionCommandService routineExecutionCommandService;

    @Operation(summary = "루틴 실행 결과 저장", description = "단일 루틴의 실행 결과를 저장합니다.")
    @PostMapping()
    public ApiResponse<RoutineExecutionResponseDTO.RoutineExecutionResultRes> createRoutineExecution(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RoutineExecutionRequestDTO.RoutineExecutionResultReq request
    ){

        return ApiResponse.of(SuccessStatus._CREATED,
                routineExecutionCommandService.saveExecutionResult(member.memberId(),request));
    }

}
