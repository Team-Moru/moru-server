package com.moru.server.domain.tts.converter;

import java.util.List;

import org.springframework.util.StringUtils;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.entity.enums.TtsAudioStatus;

public class TTSConverter {

    public static TTSResponseDTO.VoiceListResponse toVoiceListResponse(
            List<TTS> voices,
            String publicAssetBaseUrl
    ) {
        return TTSResponseDTO.VoiceListResponse.builder()
                .voices(voices.stream()
                        .map(voice -> toVoiceResponse(voice, publicAssetBaseUrl))
                        .toList())
                .build();
    }

    public static TTSResponseDTO.VoiceResponse toVoiceResponse(
            TTS voice,
            String publicAssetBaseUrl
    ) {
        return TTSResponseDTO.VoiceResponse.builder()
                .ttsId(voice.getId())
                .voiceCode(voice.getName())
                .displayName(voice.getLabel())
                .description(voice.getDescription())
                .previewAudioUrl(resolvePublicAssetUrl(publicAssetBaseUrl, voice.getPreviewAudioKey()))
                .previewAudioStatus(resolveAudioStatus(voice.getPreviewAudioKey()))
                .doneAudioUrl(resolvePublicAssetUrl(publicAssetBaseUrl, voice.getDoneAudioKey()))
                .doneAudioStatus(resolveAudioStatus(voice.getDoneAudioKey()))
                .remindAudioUrl(resolvePublicAssetUrl(publicAssetBaseUrl, voice.getRemindAudioKey()))
                .remindAudioStatus(resolveAudioStatus(voice.getRemindAudioKey()))
                .selectionVersion(voice.getSelectionVersion())
                .proOnly(voice.getIsProOnly())
                .build();
    }

    private static TtsAudioStatus resolveAudioStatus(String objectKey) {
        return StringUtils.hasText(objectKey)
                ? TtsAudioStatus.READY
                : TtsAudioStatus.PENDING;
    }

    private static String resolvePublicAssetUrl(String baseUrl, String objectKey) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(objectKey)) {
            return null;
        }

        String normalizedBaseUrl = baseUrl.replaceAll("/+$", "");
        String normalizedObjectKey = objectKey.replaceFirst("^/+", "");

        return normalizedBaseUrl + "/" + normalizedObjectKey;
    }
}
