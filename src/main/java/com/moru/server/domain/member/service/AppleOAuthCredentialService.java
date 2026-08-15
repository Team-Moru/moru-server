package com.moru.server.domain.member.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.member.entity.AppleOAuthCredential;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.AppleOAuthCredentialRepository;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.security.AppleTokenCipher;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppleOAuthCredentialService {

    private final AppleOAuthCredentialRepository credentialRepository;
    private final MemberRepository memberRepository;
    private final AppleTokenCipher tokenCipher;

    @Transactional
    public void saveOrUpdate(Long memberId, String refreshToken) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));
        String encryptedRefreshToken = tokenCipher.encrypt(refreshToken);

        credentialRepository.findByMember_Id(memberId)
                .ifPresentOrElse(
                        credential -> credential.updateEncryptedRefreshToken(encryptedRefreshToken),
                        () -> credentialRepository.save(AppleOAuthCredential.builder()
                                .member(member)
                                .encryptedRefreshToken(encryptedRefreshToken)
                                .build())
                );
    }

    @Transactional(readOnly = true)
    public Optional<String> findRefreshToken(Long memberId) {
        return credentialRepository.findByMember_Id(memberId)
                .map(AppleOAuthCredential::getEncryptedRefreshToken)
                .map(tokenCipher::decrypt);
    }

    @Transactional
    public void deleteByMemberId(Long memberId) {
        credentialRepository.deleteAllByMember_Id(memberId);
    }
}
