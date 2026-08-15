package com.moru.server.domain.member.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.moru.server.global.config.OAuthProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class AppleOAuthTokenClient {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String TOKEN_TYPE_HINT_REFRESH_TOKEN = "refresh_token";

    private final RestClient restClient;
    private final OAuthProperties oauthProperties;
    private final AppleClientSecretGenerator clientSecretGenerator;

    public AppleOAuthTokenClient(
            RestClient.Builder restClientBuilder,
            OAuthProperties oauthProperties,
            AppleClientSecretGenerator clientSecretGenerator
    ) {
        this.restClient = restClientBuilder.build();
        this.oauthProperties = oauthProperties;
        this.clientSecretGenerator = clientSecretGenerator;
    }

    public AppleTokens exchangeAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new BusinessException(ErrorStatus.APPLE_AUTHORIZATION_CODE_INVALID);
        }

        OAuthProperties.Apple apple = oauthProperties.getApple();
        MultiValueMap<String, String> form = commonForm(apple);
        form.add("code", authorizationCode);
        form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);

        try {
            AppleTokenResponse response = restClient.post()
                    .uri(apple.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);

            if (response == null
                    || !StringUtils.hasText(response.refreshToken())
                    || !StringUtils.hasText(response.idToken())) {
                throw new BusinessException(ErrorStatus.APPLE_AUTHORIZATION_CODE_INVALID);
            }
            return new AppleTokens(response.refreshToken(), response.idToken());
        } catch (HttpClientErrorException exception) {
            throw new BusinessException(ErrorStatus.APPLE_AUTHORIZATION_CODE_INVALID);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorStatus.APPLE_OAUTH_UNAVAILABLE);
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorStatus.APPLE_REVOKE_FAILED);
        }

        OAuthProperties.Apple apple = oauthProperties.getApple();
        MultiValueMap<String, String> form = commonForm(apple);
        form.add("token", refreshToken);
        form.add("token_type_hint", TOKEN_TYPE_HINT_REFRESH_TOKEN);

        try {
            restClient.post()
                    .uri(apple.getRevokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorStatus.APPLE_REVOKE_FAILED);
        }
    }

    private MultiValueMap<String, String> commonForm(OAuthProperties.Apple apple) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", apple.resolveClientId());
        form.add("client_secret", clientSecretGenerator.generate());
        return form;
    }

    public record AppleTokenResponse(
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("id_token") String idToken
    ) {
    }

    public record AppleTokens(
            String refreshToken,
            String idToken
    ) {
    }
}
