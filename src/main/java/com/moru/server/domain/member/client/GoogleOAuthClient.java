package com.moru.server.domain.member.client;

import java.time.Instant;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.moru.server.global.config.OAuthProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class GoogleOAuthClient {

    private static final String GOOGLE_API_BASE_URL = "https://oauth2.googleapis.com";
    private static final String GOOGLE_TOKEN_INFO_PATH = "/tokeninfo";
    private static final String ID_TOKEN_QUERY_PARAM = "id_token";
    private static final ParameterizedTypeReference<Map<String, Object>> GOOGLE_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final OAuthProperties oauthProperties;

    public GoogleOAuthClient(
            RestClient.Builder restClientBuilder,
            OAuthProperties oauthProperties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(GOOGLE_API_BASE_URL)
                .build();
        this.oauthProperties = oauthProperties;
    }

    public GoogleMemberInfo getMemberInfo(String idToken) {
        validateOAuthConfig();

        Map<String, Object> response = requestTokenInfo(idToken);
        validateAudience(response);
        validateExpiration(response);

        return new GoogleMemberInfo(
                extractOauthId(response),
                extractNickname(response)
        );
    }

    private void validateOAuthConfig() {
        if (!oauthProperties.getGoogle().hasClientId()) {
            throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
        }
    }

    private Map<String, Object> requestTokenInfo(String idToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GOOGLE_TOKEN_INFO_PATH)
                            .queryParam(ID_TOKEN_QUERY_PARAM, idToken)
                            .build()
                    )
                    .retrieve()
                    .body(GOOGLE_RESPONSE_TYPE);

            if (response == null) {
                throw new BusinessException(ErrorStatus.INVALID_TOKEN);
            }

            return response;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private void validateAudience(Map<String, Object> response) {
        String audience = getRequiredString(response, "aud");

        if (!audience.equals(oauthProperties.getGoogle().getClientId())) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private void validateExpiration(Map<String, Object> response) {
        String expiration = getRequiredString(response, "exp");

        try {
            long expirationEpochSecond = Long.parseLong(expiration);

            if (Instant.now().getEpochSecond() >= expirationEpochSecond) {
                throw new BusinessException(ErrorStatus.INVALID_TOKEN);
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private String extractOauthId(Map<String, Object> response) {
        return getRequiredString(response, "sub");
    }

    private String extractNickname(Map<String, Object> response) {
        String name = getNullableString(response, "name");

        if (name != null) {
            return name;
        }

        return getNullableString(response, "email");
    }

    private String getRequiredString(Map<String, Object> response, String key) {
        String value = getNullableString(response, key);

        if (value == null) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return value;
    }

    private String getNullableString(Map<String, Object> response, String key) {
        Object value = response.get(key);

        if (value == null || value.toString().isBlank()) {
            return null;
        }

        return value.toString();
    }

    public record GoogleMemberInfo(
            String oauthId,
            String nickname
    ) {
    }
}
