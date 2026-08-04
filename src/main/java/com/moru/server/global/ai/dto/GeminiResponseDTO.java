package com.moru.server.global.ai.dto;

import lombok.Builder;

public record GeminiResponseDTO() {

    @Builder
    public record AiJudgeResult(
            Boolean shouldProceed,
            String aiResponse
    ){}

    @Builder
    public record AiTtsResult(
        String ttsIntro,
        String ttsDone
    ){}
}
