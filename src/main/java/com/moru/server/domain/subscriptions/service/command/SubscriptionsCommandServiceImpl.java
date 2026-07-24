package com.moru.server.domain.subscriptions.service.command;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.subscriptions.converter.SubscriptionsConverter;
import com.moru.server.domain.subscriptions.dto.SubscriptionsRequestDTO;
import com.moru.server.domain.subscriptions.dto.SubscriptionsResponseDTO;
import com.moru.server.domain.subscriptions.entity.Subscriptions;
import com.moru.server.domain.subscriptions.entity.enums.Plan;
import com.moru.server.domain.subscriptions.repository.SubscriptionsRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionsCommandServiceImpl implements SubscriptionsCommandService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final SubscriptionsRepository subscriptionsRepository;
    private final MemberRepository memberRepository;

    @Override
    public SubscriptionsResponseDTO.SubscriptionCreateResponse createSubscription(
            Long memberId, SubscriptionsRequestDTO.SubscriptionCreateRequest request
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        Subscriptions subscription = subscriptionsRepository.findByMember_Id(memberId).orElse(null);

        if (subscription != null) {
            LocalDateTime base = (subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now))
                    ? subscription.getExpiresAt()
                    : now;
            subscription.renew(base.plusMonths(1), request.store(), request.storeTransactionId());
        } else {
            subscription = subscriptionsRepository.save(Subscriptions.builder()
                    .plan(Plan.PRO)
                    .startedAt(now)
                    .expiresAt(now.plusMonths(1))
                    .store(request.store())
                    .storeTransactionId(request.storeTransactionId())
                    .member(member)
                    .build());
        }

        return SubscriptionsConverter.toSubscriptionCreateResponse(subscription);
    }
}
