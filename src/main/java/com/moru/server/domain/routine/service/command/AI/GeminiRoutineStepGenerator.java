package com.moru.server.domain.routine.service.command.AI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.global.config.GeminiRoutineProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
public class GeminiRoutineStepGenerator implements RoutineStepGenerator {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final int MAX_STEPS_PER_ROUTINE = 5;

    private final GeminiRoutineProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiRoutineStepGenerator(
            GeminiRoutineProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(15_000);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<List<String>> generateForTimer(List<String> timerRoutineTitles) {
        if (timerRoutineTitles == null || timerRoutineTitles.isEmpty()) {
            return List.of();
        }

        String url = BASE_URL + properties.model() + ":generateContent";
        Map<String, Object> requestBody = buildRequestBody(timerRoutineTitles);

        Map<String, Object> response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", properties.apiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw stepGenerationFailed(FailureReason.EMPTY_RESPONSE_BODY);
        }

        return parseResponse(response, timerRoutineTitles.size());
    }

    private Map<String, Object> buildRequestBody(List<String> titles) {
        StringBuilder titleLines = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            titleLines.append("[").append(i).append("] ").append(titles.get(i)).append("\n");
        }

        String prompt = """
        아래 각 타이머형 루틴을 실제로 따라 할 수 있는 세부 단계(step)로 나눠줘.

        규칙:
        - 각 step은 짧고 명확한 행동 한 가지로 표현.
        - 한 루틴당 2~5개 정도.
        - 반드시 입력에 주어진 index를 그대로 사용해 결과를 반환할 것.

        예시: "스트레칭하기" -> ["목 스트레칭", "어깨 스트레칭", "허리 스트레칭"]

        루틴 목록:
        %s
        """.formatted(titleLines.toString());

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "results", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "index", Map.of("type", "INTEGER"),
                                                "steps", Map.of(
                                                        "type", "ARRAY",
                                                        "maxItems", MAX_STEPS_PER_ROUTINE,
                                                        "items", Map.of(
                                                                "type", "STRING",
                                                                "maxLength", RoutineTTS.CONTENT_MAX_LENGTH
                                                        )
                                                )
                                        ),
                                        "required", List.of("index", "steps")
                                )
                        )
                ),
                "required", List.of("results")
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
    private List<List<String>> parseResponse(Map<String, Object> response, int expectedSize) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw stepGenerationFailed(FailureReason.MISSING_CANDIDATES);
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            throw stepGenerationFailed(FailureReason.MISSING_CONTENT);
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw stepGenerationFailed(FailureReason.MISSING_PARTS);
        }

        String jsonText = (String) parts.get(0).get("text");
        if (jsonText == null) {
            throw stepGenerationFailed(FailureReason.MISSING_TEXT);
        }

        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(jsonText, Map.class);
        } catch (Exception e) {
            log.error("AI step 응답 JSON 파싱 실패. exceptionType={}",
                    e.getClass().getSimpleName());
            throw new BusinessException(ErrorStatus.ROUTINE_STEP_GENERATION_FAILED);
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) parsed.get("results");
        Map<Integer, List<String>> byIndex = new LinkedHashMap<>();
        if (results != null) {
            for (Map<String, Object> item : results) {
                Object idxObj = item.get("index");
                Object stepsObj = item.get("steps");
                if (idxObj instanceof Number idx && stepsObj instanceof List<?> steps) {
                    byIndex.put(idx.intValue(), normalizeSteps(steps, idx.intValue()));
                }
            }
        }

        List<List<String>> ordered = new ArrayList<>(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            ordered.add(byIndex.getOrDefault(i, List.of()));
        }
        return ordered;
    }

    private List<String> normalizeSteps(List<?> rawSteps, int routineIndex) {
        List<String> normalized = new ArrayList<>();

        for (Object raw : rawSteps) {
            if (normalized.size() >= MAX_STEPS_PER_ROUTINE) {
                log.warn("AI step 개수 초과 - {}개까지만 사용합니다. routineIndex={}, 응답개수={}",
                        MAX_STEPS_PER_ROUTINE, routineIndex, rawSteps.size());
                break;
            }
            if (raw == null) {
                continue;
            }

            String step = String.valueOf(raw).trim();
            if (step.isBlank()) {
                continue;
            }
            if (step.length() > RoutineTTS.CONTENT_MAX_LENGTH) {
                log.warn("AI step 길이 초과 - {}자로 자릅니다. routineIndex={}, 원본길이={}",
                        RoutineTTS.CONTENT_MAX_LENGTH, routineIndex, step.length());
                step = step.substring(0, RoutineTTS.CONTENT_MAX_LENGTH);
            }

            normalized.add(step);
        }

        return normalized;
    }

    private BusinessException stepGenerationFailed(FailureReason reason) {
        log.error("루틴 step 생성 실패. reason={}", reason);
        return new BusinessException(ErrorStatus.ROUTINE_STEP_GENERATION_FAILED);
    }

    private enum FailureReason {
        EMPTY_RESPONSE_BODY,
        MISSING_CANDIDATES,
        MISSING_CONTENT,
        MISSING_PARTS,
        MISSING_TEXT
    }
}
