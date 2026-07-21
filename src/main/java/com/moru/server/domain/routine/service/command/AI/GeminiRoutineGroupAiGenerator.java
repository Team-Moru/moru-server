package com.moru.server.domain.routine.service.command.AI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moru.server.domain.routine.dto.RoutineGroupAiGenerateResponseDTO;
import com.moru.server.global.config.GeminiRoutineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiRoutineGroupAiGenerator implements RoutineGroupAiGenerator {

    private final GeminiRoutineProperties properties;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override
    public RoutineGroupAiGenerateResponseDTO generate(String userInput) {
        String url = BASE_URL + properties.model() + ":generateContent";

        Map<String, Object> requestBody = buildRequestBody(userInput);

        Map<String, Object> response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", properties.apiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

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

    @SuppressWarnings("unchecked")
    private RoutineGroupAiGenerateResponseDTO parseResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String jsonText = (String) parts.get(0).get("text");

            return objectMapper.readValue(jsonText, RoutineGroupAiGenerateResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 파싱에 실패했습니다.", e);
        }
    }
}