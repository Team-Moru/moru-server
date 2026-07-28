package com.moru.server.global.ai;

import com.moru.server.global.ai.dto.GeminiResponseDTO;

public interface AiClient {

    GeminiResponseDTO.AiJudgeResult judge(String userInput);
    GeminiResponseDTO.AiTtsResult generateTtsScript(String userInput);
}
