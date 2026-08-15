package com.moru.server.domain.member.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppleMemberPersistenceService {

    private final MemberRepository memberRepository;
    private final AppleOAuthCredentialService credentialService;

    @Transactional
    public AppleMemberResult findOrCreateAndSaveCredential(
            String oauthId,
            String nickname,
            String refreshToken
    ) {
        Optional<Member> existingMember = memberRepository
                .findByLoginTypeAndOauthId(LoginType.APPLE, oauthId);
        Member member = existingMember.orElseGet(() -> memberRepository.saveAndFlush(Member.builder()
                .oauthId(oauthId)
                .nickname(nickname)
                .role(Role.MEMBER)
                .loginType(LoginType.APPLE)
                .build()));

        credentialService.saveOrUpdate(member.getId(), refreshToken);
        return new AppleMemberResult(member, existingMember.isEmpty());
    }

    public record AppleMemberResult(
            Member member,
            boolean isNewMember
    ) {
    }
}
