package com.moru.server.domain.subscriptions.service.query;

import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.subscriptions.converter.SubscriptionsConverter;
import com.moru.server.domain.subscriptions.dto.SubscriptionsResponseDTO;
import com.moru.server.domain.subscriptions.entity.Subscriptions;
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
@Transactional(readOnly = true)
public class SubscriptionsQueryServiceImpl implements SubscriptionsQueryService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final SubscriptionsRepository subscriptionsRepository;
    private final MemberRepository memberRepository;

    @Override
    public SubscriptionsResponseDTO.SubscriptionInfoResponse getMySubscription(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Subscriptions subscription = subscriptionsRepository.findByMember_Id(memberId).orElse(null);
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        return SubscriptionsConverter.toSubscriptionInfoResponse(subscription, now);
    }
}
