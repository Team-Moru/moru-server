package com.moru.server.domain.member.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class KakaoOAuthClient {

    private static final String KAKAO_API_BASE_URL = "https://kapi.kakao.com";
    private static final String KAKAO_USER_INFO_PATH = "/v2/user/me";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ParameterizedTypeReference<Map<String, Object>> KAKAO_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public KakaoOAuthClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(KAKAO_API_BASE_URL)
                .build();
    }

    public KakaoMemberInfo getMemberInfo(String accessToken) {
        Map<String, Object> response = requestMemberInfo(accessToken);

        return new KakaoMemberInfo(
                extractOauthId(response),
                extractNickname(response)
        );
    }

    private Map<String, Object> requestMemberInfo(String accessToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(KAKAO_USER_INFO_PATH)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(KAKAO_RESPONSE_TYPE);

            if (response == null) {
                throw new BusinessException(ErrorStatus.INVALID_TOKEN);
            }

            return response;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private String extractOauthId(Map<String, Object> response) {
        Object id = response.get("id");

        if (id == null || id.toString().isBlank()) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return id.toString();
    }

    private String extractNickname(Map<String, Object> response) {
        String kakaoAccountNickname = getNestedString(response, "kakao_account", "profile", "nickname");

        if (kakaoAccountNickname != null) {
            return kakaoAccountNickname;
        }

        return getNestedString(response, "properties", "nickname");
    }

    private String getNestedString(Map<String, Object> source, String... keys) {
        Object current = source;

        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }

            current = map.get(key);
        }

        if (current instanceof String value && !value.isBlank()) {
            return value;
        }

        return null;
    }

    public record KakaoMemberInfo(
            String oauthId,
            String nickname
    ) {
    }
}
