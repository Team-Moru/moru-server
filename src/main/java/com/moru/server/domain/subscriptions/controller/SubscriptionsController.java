package com.moru.server.domain.subscriptions.controller;

import com.moru.server.domain.subscriptions.dto.SubscriptionsRequestDTO;
import com.moru.server.domain.subscriptions.dto.SubscriptionsResponseDTO;
import com.moru.server.domain.subscriptions.service.command.SubscriptionsCommandService;
import com.moru.server.domain.subscriptions.service.query.SubscriptionsQueryService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscriptions", description = "구독 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/subscriptions")
public class SubscriptionsController {

    private final SubscriptionsQueryService subscriptionsQueryService;
    private final SubscriptionsCommandService subscriptionsCommandService;

    @Operation(summary = "내 구독 정보 조회", description = "현재 로그인한 사용자의 구독 플랜 및 유효 기간을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<SubscriptionsResponseDTO.SubscriptionInfoResponse> getMySubscription(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(subscriptionsQueryService.getMySubscription(member.memberId()));
    }

    @Operation(summary = "구독 등록", description = "결제 스토어 거래 정보를 받아 PRO 구독을 등록합니다. 기존 PRO 구독이 있으면 만료일을 연장합니다.")
    @PostMapping
    public ApiResponse<SubscriptionsResponseDTO.SubscriptionCreateResponse> createSubscription(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody SubscriptionsRequestDTO.SubscriptionCreateRequest request
    ) {
        return ApiResponse.onSuccess(subscriptionsCommandService.createSubscription(member.memberId(), request));
    }
}
