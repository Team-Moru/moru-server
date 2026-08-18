package com.moru.server.domain.tts.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import com.moru.server.domain.tts.entity.enums.TtsAudioStatus;

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

            @Schema(description = "목소리 미리듣기 음원 생성 상태", example = "READY")
            TtsAudioStatus previewAudioStatus,

            @Schema(
                    description = "루틴 완료 공통 음원 URL",
                    example = "https://moru-prod-preview-assets.s3.ap-northeast-2.amazonaws.com/tts/common/v1/leda-done.mp3",
                    nullable = true
            )
            String doneAudioUrl,

            @Schema(description = "루틴 완료 공통 음원 생성 상태", example = "READY")
            TtsAudioStatus doneAudioStatus,

            @Schema(
                    description = "루틴 리마인드 공통 음원 URL",
                    example = "https://moru-prod-preview-assets.s3.ap-northeast-2.amazonaws.com/tts/common/v1/leda-remind.mp3",
                    nullable = true
            )
            String remindAudioUrl,

            @Schema(description = "루틴 리마인드 공통 음원 생성 상태", example = "READY")
            TtsAudioStatus remindAudioStatus,

            @Schema(description = "음성 공통 음원 캐시 버전", example = "1")
            Integer selectionVersion,

            @Schema(description = "PRO 전용 여부", example = "false")
            Boolean proOnly
    ) {
    }
}
