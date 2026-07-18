package com.moru.server.domain.tts.converter;

import java.util.List;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;

public class TTSConverter {

    public static TTSResponseDTO.VoiceListResponse toVoiceListResponse(List<TTS> voices) {
        return TTSResponseDTO.VoiceListResponse.builder()
                .voices(voices.stream()
                        .map(TTSConverter::toVoiceResponse)
                        .toList())
                .build();
    }

    public static TTSResponseDTO.VoiceResponse toVoiceResponse(TTS voice) {
        return TTSResponseDTO.VoiceResponse.builder()
                .ttsId(voice.getId())
                .voiceCode(voice.getName())
                .displayName(voice.getLabel())
                .description(voice.getDescription())
                .proOnly(voice.getIsProOnly())
                .build();
    }
}
