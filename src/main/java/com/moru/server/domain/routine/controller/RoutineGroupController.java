package com.moru.server.domain.routine.controller;

import com.moru.server.domain.routine.service.command.RoutineGroup.RoutineGroupCommandService;
import com.moru.server.global.response.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.moru.server.domain.routine.dto.RoutineGroupRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.global.response.ApiResponse;

@Tag(name = "Routine Group", description = "루틴 그룹 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routine-groups")
public class RoutineGroupController {

    private final RoutineGroupCommandService routineGroupCommandService;

    @Operation(summary = "루틴 그룹 생성", description = "루틴 그룹과 그에 속한 루틴들을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoutineGroupResponseDTO.CreateResponse> createRoutineGroup(
            @Valid @RequestBody RoutineGroupRequestDTO.CreateRequest request
    ) {
        return ApiResponse.of(SuccessStatus._CREATED, routineGroupCommandService.createRoutineGroup(request));
    }
}
