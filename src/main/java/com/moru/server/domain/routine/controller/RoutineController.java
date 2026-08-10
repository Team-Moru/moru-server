package com.moru.server.domain.routine.controller;

import com.moru.server.domain.routine.dto.RoutineResponseDTO;
import com.moru.server.domain.routine.service.command.Routine.RoutineCommandService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Routine", description = "루틴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routines")
public class RoutineController {

    private final RoutineCommandService routineCommandService;

    @Operation(summary = "루틴 삭제", description = "루틴 항목을 삭제합니다.")
    @DeleteMapping("/{routineId}")
    public ApiResponse<RoutineResponseDTO.DeleteResponse> deleteRoutine(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long routineId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.onSuccess(routineCommandService.deleteRoutine(member.memberId(), routineId, idempotencyKey));
    }
}
