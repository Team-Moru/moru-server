package com.moru.server.global.ai.dto;

import lombok.Builder;

public record GeminiResponseDTO() {

    @Builder
    public record AiJudgeResult(
            Boolean shouldProceed,
            String aiResponse,

            /**
             * AI 호출이 실패해 fallback 값으로 채워졌는지 여부.
             * Gemini 응답 JSON에는 없는 필드라 역직렬화 시 null 로 들어오므로,
             * 아래 생성자에서 false 로 보정한다.
             */
            Boolean failed
    ){
        public AiJudgeResult {
            failed = Boolean.TRUE.equals(failed);
        }
    }

    @Builder
    public record AiTtsResult(
        String ttsIntro,
        String ttsDone
    ){}
}
