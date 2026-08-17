package com.moru.server.domain.onboarding.controller;

import com.moru.server.domain.onboarding.dto.OnboardingRequestDTO;
import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;
import com.moru.server.domain.onboarding.service.command.OnboardingCommandService;
import com.moru.server.domain.onboarding.service.query.OnboardingQueryService;
import com.moru.server.domain.routine.dto.RoutineGroupResponseDTO;
import com.moru.server.domain.routine.entity.enums.RoutineGoalType;
import com.moru.server.domain.routine.service.query.RoutineGroup.RoutineGroupQueryService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Onboarding", description = "온보딩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingQueryService onboardingQueryService;
    private final OnboardingCommandService onboardingCommandService;
    private final RoutineGroupQueryService routineGroupQueryService;

    @Operation(
            summary = "온보딩 완료",
            description = "로그인 후 서버에 동기화된 본인 소유 루틴 그룹을 기준으로 온보딩을 완료합니다."
    )
    @PostMapping("/complete")
    public ApiResponse<OnboardingResponseDTO.StatusResponse> completeOnboarding(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody OnboardingRequestDTO.CompleteRequest request
    ) {
        return ApiResponse.onSuccess(
                onboardingCommandService.completeOnboarding(member.memberId(), request));
    }

    @Operation(summary = "온보딩 완료 여부 조회", description = "현재 로그인한 사용자의 온보딩 완료 여부를 조회합니다.")
    @GetMapping("/status")
    public ApiResponse<OnboardingResponseDTO.StatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(onboardingQueryService.getOnboardingStatus(member.memberId()));
    }

    @Operation(summary = "목표별 추천 루틴 조회", description = "선택한 목표(활력/건강/마음 안정/습관 형성)에 맞는 추천 루틴 그룹을 조회합니다.")
    @GetMapping("/recommendations")
    public ApiResponse<List<RoutineGroupResponseDTO.DetailResponse>> getRecommendedRoutines(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam RoutineGoalType goalType
    ) {
        return ApiResponse.onSuccess(routineGroupQueryService.getRecommendedRoutineGroups(goalType));
    }
}
