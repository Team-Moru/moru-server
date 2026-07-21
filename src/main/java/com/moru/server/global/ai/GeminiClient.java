package com.moru.server.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moru.server.global.ai.dto.GeminiResponseDTO;
import com.moru.server.global.ai.prompt.RoutinePrompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient implements AiClient{

    @Value("${gemini.api-key:}")
    private String apiKey ;
    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient ;

    public GeminiClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override
    public GeminiResponseDTO.AiJudgeResult judge(String userInput) {
        try {

            String prompt = RoutinePrompt.judgePrompt(userInput);


            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json")
            );


            String raw = restClient.post()
                    .uri(BASE_URL + model + ":generateContent")
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);


            JsonNode root = objectMapper.readTree(raw);
            String jsonText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();


            return objectMapper.readValue(jsonText, GeminiResponseDTO.AiJudgeResult.class);

        } catch (Exception e) {
            log.error("Gemini 호출 실패, fallback 반환", e);
            return GeminiResponseDTO.AiJudgeResult.builder()
                    .shouldProceed(false)
                    .aiResponse("현재 AI응답을 불러올 수 없습니다. 다음으로 넘어갈게요")
                    .build();
        }
    }
}
