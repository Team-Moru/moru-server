package com.moru.server.domain.onboarding.controller;

import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;
import com.moru.server.domain.onboarding.service.query.OnboardingQueryService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingQueryService onboardingQueryService;

    @Operation(summary = "온보딩 완료 여부 조회", description = "현재 로그인한 사용자의 온보딩 완료 여부를 조회합니다.")
    @GetMapping("/status")
    public ApiResponse<OnboardingResponseDTO.StatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(onboardingQueryService.getOnboardingStatus(member.memberId()));
    }
}
