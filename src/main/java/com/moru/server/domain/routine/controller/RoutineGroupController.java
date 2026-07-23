package com.moru.server.domain.routine.controller;

import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;
import com.moru.server.domain.routine.service.command.AI.RoutineCommandAiService;
import com.moru.server.domain.routine.service.command.RoutineGroup.RoutineGroupCommandService;
import com.moru.server.domain.routine.service.query.RoutineGroup.RoutineGroupQueryService;
import com.moru.server.global.response.code.status.SuccessStatus;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.moru.server.domain.routine.dto.RoutineGroupRequestDTO;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.global.response.ApiResponse;

import java.util.List;

@Tag(name = "Routine Group", description = "루틴 그룹 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/routine-groups")
public class RoutineGroupController {

    private final RoutineGroupCommandService routineGroupCommandService;
    private final RoutineGroupQueryService routineGroupQueryService;
    private final RoutineCommandAiService routineCommandAiService;

    @Operation(summary = "루틴 그룹 생성", description = "루틴 그룹과 그에 속한 루틴들을 생성합니다.")
    @PostMapping
    public ApiResponse<RoutineGroupResponseDTO.DetailResponse> createRoutineGroup(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RoutineGroupRequestDTO.CreateRequest request
    ) {
        return ApiResponse.of(SuccessStatus._CREATED,
                routineGroupCommandService.createRoutineGroup(member.memberId(), request));
    }

    @Operation(summary = "루틴 그룹 상세 조회", description = "루틴 그룹 상세 정보를 조회합니다.")
    @GetMapping("/{routineGroupId}")
    public ApiResponse<RoutineGroupResponseDTO.DetailResponse> getRoutineGroupDetail(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long routineGroupId
    ) {
        return ApiResponse.onSuccess(routineGroupQueryService.getRoutineGroupDetail(member.memberId(), routineGroupId));
    }
  
    @Operation(summary = "루틴 그룹 삭제", description = "루틴 그룹을 삭제합니다.")
    @DeleteMapping("/{routineGroupId}")
    public ApiResponse<RoutineGroupResponseDTO.DeleteResponse> deleteRoutineGroup(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long routineGroupId
    ) {
        return ApiResponse.onSuccess(routineGroupCommandService.deleteRoutineGroup(member.memberId(), routineGroupId));
    }
  
    @Operation(summary = "루틴 그룹 활성화 토글", description = "루틴 그룹의 활성화 상태를 토글합니다.")
    @PatchMapping("/{routineGroupId}/active")
    public ApiResponse<RoutineGroupResponseDTO.ActiveResponse> updateActive(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long routineGroupId,
            @Valid @RequestBody RoutineGroupRequestDTO.ActiveRequest request
    ) {
        return ApiResponse.onSuccess(routineGroupCommandService.updateActive(member.memberId(), routineGroupId, request));
    }  
      
    @Operation(summary = "루틴 그룹 목록 조회", description = "내 루틴 그룹 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<RoutineGroupResponseDTO.SummaryResponse>> getRoutineGroups(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(routineGroupQueryService.getRoutineGroups(member.memberId()));
    }

    @Operation(summary = "오늘의 루틴 진행 현황 조회", description = "사용 중인 루틴 그룹의 오늘 완료 현황을 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<RoutineGroupResponseDTO.TodayResponse> getTodayRoutine(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(routineGroupQueryService.getTodayRoutine(member.memberId()));
    }

    @Operation(summary = "사용 중인 루틴 그룹 조회", description = "사용 중인(is_active) 루틴 그룹과 오늘 진행 현황을 조회합니다.")
    @GetMapping("/active")
    public ApiResponse<RoutineGroupResponseDTO.ActiveRoutineResponse> getActiveRoutine(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(routineGroupQueryService.getActiveRoutine(member.memberId()));
    }

    @Operation(summary = "루틴 항목 추가", description = "루틴 그룹에 루틴 항목을 추가합니다.")
    @PostMapping("/{routineGroupId}/routines")
    public ApiResponse<RoutineGroupResponseDTO.RoutineResponse> addRoutine(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long routineGroupId,
            @Valid @RequestBody RoutineGroupRequestDTO.RoutineRequest request
    ) {
        return ApiResponse.of(SuccessStatus._CREATED,
                routineGroupCommandService.addRoutine(member.memberId(), routineGroupId, request));
    }

    @Operation(summary = "AI 루틴 그룹 생성", description = "자연어 입력을 기반으로 AI가 루틴 그룹을 생성합니다.")
    @PostMapping("/ai-generate")
    public ApiResponse<RoutineGroupAiGenerateResponseDTO> generateAiRoutine(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RoutineGroupAiGenerateRequestDTO request
    ) {
        return ApiResponse.of(SuccessStatus._CREATED,
                routineCommandAiService.generateRoutineGroup(request.userInput()));
    }
}
