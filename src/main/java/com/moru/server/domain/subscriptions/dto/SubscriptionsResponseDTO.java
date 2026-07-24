package com.moru.server.domain.subscriptions.dto;

import com.moru.server.domain.subscriptions.entity.enums.Plan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record SubscriptionsResponseDTO() {

    @Builder
    @Schema(description = "내 구독 정보 조회 응답")
    public record SubscriptionInfoResponse(
            @Schema(description = "구독 플랜", example = "PRO")
            Plan plan,

            @Schema(description = "구독 시작 시간", example = "2026-06-01T00:00:00", nullable = true)
            LocalDateTime startedAt,

            @Schema(description = "구독 만료 시간 (FREE면 null)", example = "2026-08-01T00:00:00", nullable = true)
            LocalDateTime expiresAt,

            @Schema(description = "현재 구독 유효 여부", example = "true")
            Boolean isActive
    ) {
    }
}
