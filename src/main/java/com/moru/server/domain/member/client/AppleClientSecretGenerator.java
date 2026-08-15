package com.moru.server.domain.member.client;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.moru.server.global.config.OAuthProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final long CLIENT_SECRET_VALID_DAYS = 30;

    private final OAuthProperties oauthProperties;

    public String generate() {
        OAuthProperties.Apple apple = oauthProperties.getApple();
        validateConfig(apple);

        Instant now = Instant.now();

        return Jwts.builder()
                .header()
                .keyId(apple.getKeyId())
                .and()
                .issuer(apple.getTeamId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(CLIENT_SECRET_VALID_DAYS, ChronoUnit.DAYS)))
                .audience()
                .add(APPLE_AUDIENCE)
                .and()
                .subject(apple.resolveClientId())
                .signWith(parsePrivateKey(apple.getPrivateKey()), Jwts.SIG.ES256)
                .compact();
    }

    private void validateConfig(OAuthProperties.Apple apple) {
        if (!apple.hasTokenApiConfig()) {
            throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) {
        try {
            String normalizedPem = privateKeyPem
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalizedPem);
            return KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
        }
    }
}
