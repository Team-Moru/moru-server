package com.moru.server.domain.routine.controller;

import com.moru.server.domain.routine.dto.RoutineTTSResponseDTO;
import com.moru.server.domain.routine.service.query.RoutineTTS.RoutineTTSQueryService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "RoutineTTS", description = "루틴 TTS API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routine-tts")
public class RoutineTTSController {

    private final RoutineTTSQueryService routineTTSQueryService;


    @Operation(summary = "루틴 TTS 조회", description = "루틴 실행에 필요한 TTS을 조회합니다")
    @GetMapping("/{routineGroupId}/tts")
    public ApiResponse<List<RoutineTTSResponseDTO.RoutineTTSRes>> getMonthlyExecutions(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long routineGroupId
    ){

        return ApiResponse.onSuccess(
                routineTTSQueryService.getRoutineTTS(routineGroupId,member.memberId()));

    }

}
