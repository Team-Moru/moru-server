package com.moru.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.moru.server.domain.member.repository.AppleOAuthCredentialRepository;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@SpringBootTest(properties = "security.apple-token.encryption-key=aW52YWxpZA==")
@ActiveProfiles("test")
class AppleMemberPersistenceServiceTest {

    @Autowired
    private AppleMemberPersistenceService persistenceService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AppleOAuthCredentialRepository credentialRepository;

    @AfterEach
    void cleanUp() {
        credentialRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void rollsBackNewMemberWhenCredentialEncryptionFails() {
        assertThatThrownBy(() -> persistenceService.findOrCreateAndSaveCredential(
                "apple-member-id",
                "moru@example.com",
                "apple-refresh-token"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.OAUTH_CONFIG_MISSING));

        assertThat(memberRepository.findByLoginTypeAndOauthId(
                com.moru.server.domain.member.entity.enums.LoginType.APPLE,
                "apple-member-id"
        )).isEmpty();
        assertThat(credentialRepository.count()).isZero();
    }
}
