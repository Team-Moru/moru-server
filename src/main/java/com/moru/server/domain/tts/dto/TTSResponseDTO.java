package com.moru.server.domain.tts.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record TTSResponseDTO() {

    @Builder
    @Schema(description = "TTS 목소리 목록 조회 응답")
    public record VoiceListResponse(
            @Schema(description = "선택 가능한 목소리 목록")
            List<VoiceResponse> voices
    ) {
    }

    @Builder
    @Schema(description = "TTS 목소리 응답")
    public record VoiceResponse(
            @Schema(description = "TTS ID", example = "1")
            Long ttsId,

            @Schema(description = "목소리 코드명", example = "MINSEO")
            String voiceCode,

            @Schema(description = "목소리 표시명", example = "민서")
            String displayName,

            @Schema(description = "목소리 설명", example = "따뜻한 친구")
            String description,

            @Schema(
                    description = "목소리 미리듣기 음원 URL",
                    example = "https://moru-prod-preview-assets.s3.ap-northeast-2.amazonaws.com/tts/previews/v1/leda.mp3"
            )
            String previewAudioUrl,

            @Schema(description = "PRO 전용 여부", example = "false")
            Boolean proOnly
    ) {
    }
}
