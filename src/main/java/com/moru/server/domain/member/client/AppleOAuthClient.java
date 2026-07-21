package com.moru.server.domain.member.client;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.moru.server.global.config.OAuthProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class AppleOAuthClient {

    private static final String RSA_KEY_TYPE = "RSA";
    private static final ParameterizedTypeReference<Map<String, Object>> APPLE_JWKS_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final OAuthProperties oauthProperties;

    public AppleOAuthClient(
            RestClient.Builder restClientBuilder,
            OAuthProperties oauthProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.oauthProperties = oauthProperties;
    }

    public AppleMemberInfo getMemberInfo(String identityToken) {
        validateOAuthConfig();

        Claims claims = parseClaims(identityToken);

        return new AppleMemberInfo(
                getRequiredSubject(claims),
                extractNickname(claims)
        );
    }

    private void validateOAuthConfig() {
        if (!oauthProperties.getApple().hasAudience()) {
            throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
        }
    }

    private Claims parseClaims(String identityToken) {
        try {
            return Jwts.parser()
                    .keyLocator(new LocatorAdapter<>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return getPublicKey(header.getKeyId());
                        }
                    })
                    .requireIssuer(oauthProperties.getApple().getIssuer())
                    .requireAudience(oauthProperties.getApple().getAudience())
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private PublicKey getPublicKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        Map<String, Object> jwks = requestJwks();
        Map<?, ?> jwk = findJwk(jwks, keyId);

        return createPublicKey(jwk);
    }

    private Map<String, Object> requestJwks() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(oauthProperties.getApple().getJwksUri())
                    .retrieve()
                    .body(APPLE_JWKS_RESPONSE_TYPE);

            if (response == null) {
                throw new BusinessException(ErrorStatus.INVALID_TOKEN);
            }

            return response;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private Map<?, ?> findJwk(Map<String, Object> jwks, String keyId) {
        Object keys = jwks.get("keys");

        if (!(keys instanceof List<?> keyList)) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return keyList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(jwk -> keyId.equals(getRequiredString(jwk, "kid")))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorStatus.INVALID_TOKEN));
    }

    private PublicKey createPublicKey(Map<?, ?> jwk) {
        try {
            BigInteger modulus = decodeBase64UrlInteger(getRequiredString(jwk, "n"));
            BigInteger exponent = decodeBase64UrlInteger(getRequiredString(jwk, "e"));
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);

            return KeyFactory.getInstance(RSA_KEY_TYPE).generatePublic(keySpec);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private BigInteger decodeBase64UrlInteger(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

    private String extractNickname(Claims claims) {
        return claims.get("email", String.class);
    }

    private String getRequiredSubject(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return subject;
    }

    private String getRequiredString(Map<?, ?> source, String key) {
        Object value = source.get(key);

        if (value == null || value.toString().isBlank()) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return value.toString();
    }

    public record AppleMemberInfo(
            String oauthId,
            String nickname
    ) {
    }
}
