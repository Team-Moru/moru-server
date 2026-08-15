package com.moru.server.domain.member.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moru.server.global.config.AppleTokenEncryptionProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

class AppleTokenCipherTest {

    private AppleTokenCipher tokenCipher;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        AppleTokenEncryptionProperties properties = new AppleTokenEncryptionProperties();
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(key));
        tokenCipher = new AppleTokenCipher(properties);
    }

    @Test
    void encryptsAndDecryptsRefreshToken() {
        String encrypted = tokenCipher.encrypt("apple-refresh-token");

        assertThat(encrypted).startsWith("v1:");
        assertThat(encrypted).doesNotContain("apple-refresh-token");
        assertThat(tokenCipher.decrypt(encrypted)).isEqualTo("apple-refresh-token");
    }

    @Test
    void usesDifferentIvForEveryEncryption() {
        String first = tokenCipher.encrypt("apple-refresh-token");
        String second = tokenCipher.encrypt("apple-refresh-token");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsInvalidEncryptionKeyLength() {
        AppleTokenEncryptionProperties properties = new AppleTokenEncryptionProperties();
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[16]));
        AppleTokenCipher invalidCipher = new AppleTokenCipher(properties);

        assertThatThrownBy(() -> invalidCipher.encrypt("apple-refresh-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.OAUTH_CONFIG_MISSING));
    }
}
