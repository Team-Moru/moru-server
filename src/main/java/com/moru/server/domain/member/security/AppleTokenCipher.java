package com.moru.server.domain.member.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.moru.server.global.config.AppleTokenEncryptionProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppleTokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String VERSION_PREFIX = "v1:";
    private static final byte[] AAD = "moru:apple-refresh-token:v1".getBytes(StandardCharsets.UTF_8);
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final AppleTokenEncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            throw new BusinessException(ErrorStatus.APPLE_CREDENTIAL_INVALID);
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(AAD);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorStatus.APPLE_CREDENTIAL_INVALID);
        }
    }

    public String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText) || !encryptedText.startsWith(VERSION_PREFIX)) {
            throw new BusinessException(ErrorStatus.APPLE_CREDENTIAL_INVALID);
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText.substring(VERSION_PREFIX.length()));
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new BusinessException(ErrorStatus.APPLE_CREDENTIAL_INVALID);
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorStatus.APPLE_CREDENTIAL_INVALID);
        }
    }

    private SecretKey encryptionKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(properties.getEncryptionKey());
            if (decodedKey.length != KEY_LENGTH_BYTES) {
                throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
            }
            return new SecretKeySpec(decodedKey, KEY_ALGORITHM);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
        }
    }
}
