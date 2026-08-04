package com.moru.server.domain.subscriptions.service.command;

import com.moru.server.domain.subscriptions.dto.SubscriptionsRequestDTO;
import com.moru.server.domain.subscriptions.dto.SubscriptionsResponseDTO;

public interface SubscriptionsCommandService {

    SubscriptionsResponseDTO.SubscriptionCreateResponse createSubscription(
            Long memberId, SubscriptionsRequestDTO.SubscriptionCreateRequest request
    );
}
