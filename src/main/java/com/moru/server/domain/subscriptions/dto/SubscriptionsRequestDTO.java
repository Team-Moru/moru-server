package com.moru.server.domain.subscriptions.dto;

import com.moru.server.domain.subscriptions.entity.enums.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscriptionsRequestDTO() {

    @Schema(description = "구독 등록 요청")
    public record SubscriptionCreateRequest(
            @NotNull(message = "store는 필수입니다.")
            @Schema(description = "결제 스토어", example = "APP_STORE")
            Store store,

            @NotBlank(message = "storeTransactionId는 필수입니다.")
            @Schema(description = "스토어 거래 ID", example = "1000000012345678")
            String storeTransactionId
    ) {
    }
}
