package com.moru.server.domain.subscriptions.service.query;

import com.moru.server.domain.subscriptions.dto.SubscriptionsResponseDTO;

public interface SubscriptionsQueryService {

    SubscriptionsResponseDTO.SubscriptionInfoResponse getMySubscription(Long memberId);
}
