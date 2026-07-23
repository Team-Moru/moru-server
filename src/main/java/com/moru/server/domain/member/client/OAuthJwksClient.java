package com.moru.server.domain.member.client;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class OAuthJwksClient {

    private static final String RSA_KEY_TYPE = "RSA";
    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(1);
    private static final ParameterizedTypeReference<Map<String, Object>> JWKS_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final ConcurrentMap<String, CachedJwks> jwksCache = new ConcurrentHashMap<>();

    public OAuthJwksClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public PublicKey getPublicKey(String jwksUri, String keyId) {
        validateKeyRequest(jwksUri, keyId);

        Optional<Map<?, ?>> cachedJwk = findJwk(getJwks(jwksUri), keyId);
        Map<?, ?> jwk = cachedJwk.orElseGet(() -> findJwk(refreshJwks(jwksUri), keyId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.INVALID_TOKEN)));

        return createPublicKey(jwk);
    }

    private void validateKeyRequest(String jwksUri, String keyId) {
        if (jwksUri == null || jwksUri.isBlank() || keyId == null || keyId.isBlank()) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private Map<String, Object> getJwks(String jwksUri) {
        CachedJwks cachedJwks = jwksCache.get(jwksUri);

        if (cachedJwks != null && !cachedJwks.isExpired()) {
            return cachedJwks.jwks();
        }

        return refreshExpiredJwks(jwksUri);
    }

    private synchronized Map<String, Object> refreshExpiredJwks(String jwksUri) {
        CachedJwks cachedJwks = jwksCache.get(jwksUri);

        if (cachedJwks != null && !cachedJwks.isExpired()) {
            return cachedJwks.jwks();
        }

        return refreshJwks(jwksUri);
    }

    private synchronized Map<String, Object> refreshJwks(String jwksUri) {
        Map<String, Object> jwks = requestJwks(jwksUri);
        jwksCache.put(jwksUri, new CachedJwks(jwks, Instant.now().plus(JWKS_CACHE_TTL)));
        return jwks;
    }

    private Map<String, Object> requestJwks(String jwksUri) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(jwksUri)
                    .retrieve()
                    .body(JWKS_RESPONSE_TYPE);

            if (response == null) {
                throw new BusinessException(ErrorStatus.INVALID_TOKEN);
            }

            return response;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private Optional<Map<?, ?>> findJwk(Map<String, Object> jwks, String keyId) {
        Object keys = jwks.get("keys");

        if (!(keys instanceof List<?> keyList)) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        for (Object candidate : keyList) {
            if (candidate instanceof Map<?, ?> jwk
                    && keyId.equals(getNullableString(jwk, "kid"))) {
                return Optional.of(jwk);
            }
        }

        return Optional.empty();
    }

    private PublicKey createPublicKey(Map<?, ?> jwk) {
        if (!RSA_KEY_TYPE.equals(getRequiredString(jwk, "kty"))) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

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

    private String getRequiredString(Map<?, ?> source, String key) {
        String value = getNullableString(source, key);

        if (value == null) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return value;
    }

    private String getNullableString(Map<?, ?> source, String key) {
        Object value = source.get(key);

        if (value == null || value.toString().isBlank()) {
            return null;
        }

        return value.toString();
    }

    private record CachedJwks(
            Map<String, Object> jwks,
            Instant expiresAt
    ) {

        private boolean isExpired() {
            return !Instant.now().isBefore(expiresAt);
        }
    }
}
