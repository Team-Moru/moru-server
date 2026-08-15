package com.moru.server.domain.member.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.AppleOAuthCredentialRepository;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.repository.MemberTermRepository;
import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.domain.subscriptions.repository.SubscriptionsRepository;

@ExtendWith(MockitoExtension.class)
class MemberDeletionTransactionServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberTermRepository memberTermRepository;

    @Mock
    private AppleOAuthCredentialRepository appleOAuthCredentialRepository;

    @Mock
    private RoutineGroupRepository routineGroupRepository;

    @Mock
    private SubscriptionsRepository subscriptionsRepository;

    @InjectMocks
    private MemberDeletionTransactionService deletionService;

    @Test
    void deletesChildRowsBeforeMember() {
        Member member = Member.builder().id(1L).build();
        RoutineGroup routineGroup = RoutineGroup.builder().id(10L).build();
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        when(routineGroupRepository.findAllByMember_Id(1L)).thenReturn(List.of(routineGroup));

        deletionService.deleteMemberData(1L);

        InOrder order = inOrder(
                routineGroupRepository,
                subscriptionsRepository,
                memberTermRepository,
                appleOAuthCredentialRepository,
                memberRepository
        );
        order.verify(routineGroupRepository).deleteAll(List.of(routineGroup));
        order.verify(subscriptionsRepository).deleteAllByMember_Id(1L);
        order.verify(memberTermRepository).deleteAllByMember_Id(1L);
        order.verify(appleOAuthCredentialRepository).deleteAllByMember_Id(1L);
        order.verify(memberRepository).delete(member);
        order.verify(memberRepository).flush();
    }

    @Test
    void doesNothingWhenMemberWasAlreadyDeleted() {
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        deletionService.deleteMemberData(1L);

        verify(routineGroupRepository, never()).findAllByMember_Id(1L);
        verify(memberRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
