package com.moru.server.domain.subscriptions.converter;

import com.moru.server.domain.subscriptions.dto.SubscriptionsResponseDTO;
import com.moru.server.domain.subscriptions.entity.Subscriptions;
import com.moru.server.domain.subscriptions.entity.enums.Plan;

import java.time.LocalDateTime;

public class SubscriptionsConverter {

    public static SubscriptionsResponseDTO.SubscriptionInfoResponse toSubscriptionInfoResponse(
            Subscriptions subscription, LocalDateTime now
    ) {
        if (subscription == null) {
            return SubscriptionsResponseDTO.SubscriptionInfoResponse.builder()
                    .plan(Plan.FREE)
                    .startedAt(null)
                    .expiresAt(null)
                    .isActive(false)
                    .build();
        }

        boolean isActive = subscription.getPlan() == Plan.PRO
                && (subscription.getExpiresAt() == null || subscription.getExpiresAt().isAfter(now));

        return SubscriptionsResponseDTO.SubscriptionInfoResponse.builder()
                .plan(subscription.getPlan())
                .startedAt(subscription.getStartedAt())
                .expiresAt(subscription.getExpiresAt())
                .isActive(isActive)
                .build();
    }
}
