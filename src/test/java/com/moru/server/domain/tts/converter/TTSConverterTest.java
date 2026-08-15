package com.moru.server.domain.tts.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;

class TTSConverterTest {

    private static final String PUBLIC_ASSET_BASE_URL =
            "https://moru-prod-preview-assets.s3.ap-northeast-2.amazonaws.com";

    @Test
    void convertsPreviewAudioKeyToPublicUrl() {
        TTS voice = createVoice(1L, "Leda", "tts/previews/v1/leda.mp3");

        TTSResponseDTO.VoiceListResponse response = TTSConverter.toVoiceListResponse(
                List.of(voice),
                PUBLIC_ASSET_BASE_URL + "/"
        );

        assertThat(response.voices()).singleElement().satisfies(result -> {
            assertThat(result.ttsId()).isEqualTo(1L);
            assertThat(result.voiceCode()).isEqualTo("Leda");
            assertThat(result.previewAudioUrl()).isEqualTo(
                    PUBLIC_ASSET_BASE_URL + "/tts/previews/v1/leda.mp3"
            );
        });
    }

    @Test
    void returnsNullPreviewUrlWhenObjectKeyIsMissing() {
        TTS voice = createVoice(1L, "Leda", null);

        TTSResponseDTO.VoiceListResponse response = TTSConverter.toVoiceListResponse(
                List.of(voice),
                PUBLIC_ASSET_BASE_URL
        );

        assertThat(response.voices()).singleElement().satisfies(result ->
                assertThat(result.previewAudioUrl()).isNull()
        );
    }

    @Test
    void returnsNullPreviewUrlWhenBaseUrlIsMissing() {
        TTS voice = createVoice(1L, "Leda", "tts/previews/v1/leda.mp3");

        TTSResponseDTO.VoiceListResponse response = TTSConverter.toVoiceListResponse(
                List.of(voice),
                ""
        );

        assertThat(response.voices()).singleElement().satisfies(result ->
                assertThat(result.previewAudioUrl()).isNull()
        );
    }

    private TTS createVoice(Long id, String name, String previewAudioKey) {
        return TTS.builder()
                .id(id)
                .name(name)
                .label(name)
                .description("미리듣기 테스트 음성")
                .previewAudioKey(previewAudioKey)
                .isProOnly(false)
                .build();
    }
}
