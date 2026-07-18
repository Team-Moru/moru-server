package com.moru.server.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moru.server.global.ai.dto.GeminiResponseDTO;
import com.moru.server.global.ai.prompt.RoutinePrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient implements AiClient{

    @Value("${gemini.api-key}")
    private String apiKey ;
    @Value("${gemini.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();


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
                    .uri(BASE_URL + model + ":generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);


            JsonNode root = objectMapper.readTree(raw);
            String jsonText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();


            return objectMapper.readValue(jsonText, GeminiResponseDTO.AiJudgeResult.class);

        } catch (Exception e) {

            return GeminiResponseDTO.AiJudgeResult.builder()
                    .shouldProceed(true)
                    .aiResponse("현재 AI응답을 불러올 수 없습니다. 다음으로 넘어갈게요")
                    .build();
        }
    }
}
