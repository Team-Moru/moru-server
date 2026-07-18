package com.moru.server.global.ai.dto;

import lombok.Builder;

public class GeminiResponseDTO {

    @Builder
    public record AiJudgeResult(
            Boolean shouldProceed,
            String aiResponse
    ){}
}
