package com.moru.server.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.AppleOAuthCredentialRepository;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.repository.MemberTermRepository;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.domain.subscriptions.repository.SubscriptionsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberDeletionTransactionService {

    private final MemberRepository memberRepository;
    private final MemberTermRepository memberTermRepository;
    private final AppleOAuthCredentialRepository appleOAuthCredentialRepository;
    private final RoutineGroupRepository routineGroupRepository;
    private final SubscriptionsRepository subscriptionsRepository;

    @Transactional
    public void deleteMemberData(Long memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId).orElse(null);
        if (member == null) {
            return;
        }

        routineGroupRepository.deleteAll(routineGroupRepository.findAllByMember_Id(memberId));
        subscriptionsRepository.deleteAllByMember_Id(memberId);
        memberTermRepository.deleteAllByMember_Id(memberId);
        appleOAuthCredentialRepository.deleteAllByMember_Id(memberId);
        memberRepository.delete(member);
        memberRepository.flush();
    }
}
