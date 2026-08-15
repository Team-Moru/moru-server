package com.moru.server.domain.tts.converter;

import java.util.List;

import org.springframework.util.StringUtils;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;

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
                .proOnly(voice.getIsProOnly())
                .build();
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
