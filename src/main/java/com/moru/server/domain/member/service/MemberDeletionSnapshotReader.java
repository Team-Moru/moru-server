package com.moru.server.domain.member.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberDeletionSnapshotReader {

    private final MemberRepository memberRepository;
    private final RoutineTTSRepository routineTTSRepository;

    @Transactional(readOnly = true)
    public Optional<MemberDeletionSnapshot> findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .map(member -> toSnapshot(member, routineTTSRepository.findS3KeysByMemberId(memberId)));
    }

    private MemberDeletionSnapshot toSnapshot(Member member, List<String> ttsKeys) {
        return new MemberDeletionSnapshot(
                member.getId(),
                member.getLoginType(),
                member.getOauthId(),
                member.getProfileImageKey(),
                List.copyOf(ttsKeys)
        );
    }

    public record MemberDeletionSnapshot(
            Long memberId,
            LoginType loginType,
            String oauthId,
            String profileImageKey,
            List<String> ttsKeys
    ) {
    }
}
