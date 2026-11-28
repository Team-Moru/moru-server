package com.moru.server.domain.routine.service.command.AI;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;
import com.moru.server.global.config.GeminiRoutineProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class GeminiRoutineGroupAiGenerator implements RoutineGroupAiGenerator {


    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final GeminiRoutineProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiRoutineGroupAiGenerator(
            GeminiRoutineProperties properties,
            RestClient.Builder restClientBuilder

    ) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(15_000);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }


    @Override
    public RoutineGroupAiGenerateResponseDTO generate(String userInput) {
        String url = BASE_URL + properties.model() + ":generateContent";

        Map<String, Object> requestBody = buildRequestBody(userInput);

        byte[] response;
        try {
            response = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", properties.apiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorStatus.AI_ROUTINE_UPSTREAM_UNAVAILABLE);
        }

        return parseResponse(response);
    }

    private Map<String, Object> buildRequestBody(String userInput) {
        String prompt = """
        사용자가 원하는 루틴을 자연어로 입력하면, 이를 분석해서 루틴 그룹을 만들어줘.

        사용자 입력은 두 가지 형태일 수 있어:
        1. 구체적인 행동을 나열한 경우 (예: "물 마시고, 스트레칭 하고, 일기 쓰기")
           → 언급된 행동을 각각 하나의 루틴 항목으로 분리해줘.
        2. 추상적인 목표나 분위기만 말한 경우 (예: "부지런하게 살 수 있는 루틴 만들어줘", "아침에 활기차게 시작하고 싶어")
           → 그 목표를 달성하는 데 도움이 되는 구체적인 행동들을 3~6개 정도 새로 제안해줘.

        규칙:
        - title: 루틴 전체를 대표하는 짧은 이름 (예: "활력 루틴")
        - description: 루틴을 한 문장으로 요약한 설명
        - routines: 루틴 항목 리스트
          - title: 항목 이름 (구체적인 행동으로 표현, 예: "잠자리 정리하기")
          - type: CHECK(단순 확인), TIMER(시간 재며 진행), INPUT(직접 입력/기록) 중 하나
          - durationSecond: 예상 소요 시간(초 단위)

        사용자 입력: "%s"
        """.formatted(userInput);

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "title", Map.of("type", "STRING"),
                        "description", Map.of("type", "STRING"),
                        "routines", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "title", Map.of("type", "STRING"),
                                                "type", Map.of(
                                                        "type", "STRING",
                                                        "enum", List.of("CHECK", "TIMER", "INPUT")
                                                ),
                                                "durationSecond", Map.of("type", "INTEGER")
                                        ),
                                        "required", List.of("title", "type", "durationSecond")
                                )
                        )
                ),
                "required", List.of("title", "description", "routines")
        );

        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );
    }

    private RoutineGroupAiGenerateResponseDTO parseResponse(byte[] response) {
        if (response == null || response.length == 0) {
            throw new BusinessException(ErrorStatus.AI_ROUTINE_PARSE_FAILED);
        }
        try {
            var root = objectMapper.readTree(response);
            var text = root == null ? null : root.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text");
            if (text == null || !text.isTextual()) {
                throw new BusinessException(ErrorStatus.AI_ROUTINE_PARSE_FAILED);
            }
            var result = objectMapper.readValue(text.asText(), RoutineGroupAiGenerateResponseDTO.class);
            if (result == null || result.title() == null || result.description() == null
                    || result.routines() == null || result.routines().isEmpty()
                    || result.routines().stream().anyMatch(item -> item == null || item.title() == null
                    || item.type() == null || item.durationSecond() < 0)) {
                throw new BusinessException(ErrorStatus.AI_ROUTINE_PARSE_FAILED);
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(ErrorStatus.AI_ROUTINE_PARSE_FAILED);
        }
    }
}
