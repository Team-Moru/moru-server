package com.moru.server.domain.tts.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.entity.enums.TtsAudioStatus;

class TTSConverterTest {

    private static final String PUBLIC_ASSET_BASE_URL =
            "https://moru-prod-preview-assets.s3.ap-northeast-2.amazonaws.com";

    @Test
    void convertsAudioKeysToPublicUrls() {
        TTS voice = createVoice(
                1L,
                "Leda",
                "tts/previews/v1/leda.mp3",
                "tts/common/v1/leda-done.mp3",
                "tts/common/v1/leda-remind.mp3",
                2
        );

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
            assertThat(result.previewAudioStatus()).isEqualTo(TtsAudioStatus.READY);
            assertThat(result.doneAudioUrl()).isEqualTo(
                    PUBLIC_ASSET_BASE_URL + "/tts/common/v1/leda-done.mp3"
            );
            assertThat(result.doneAudioStatus()).isEqualTo(TtsAudioStatus.READY);
            assertThat(result.remindAudioUrl()).isEqualTo(
                    PUBLIC_ASSET_BASE_URL + "/tts/common/v1/leda-remind.mp3"
            );
            assertThat(result.remindAudioStatus()).isEqualTo(TtsAudioStatus.READY);
            assertThat(result.selectionVersion()).isEqualTo(2);
        });
    }

    @Test
    void returnsPendingStatusAndNullUrlsWhenAudioKeysAreMissing() {
        TTS voice = createVoice(1L, "Leda", null, null, null, 1);

        TTSResponseDTO.VoiceListResponse response = TTSConverter.toVoiceListResponse(
                List.of(voice),
                PUBLIC_ASSET_BASE_URL
        );

        assertThat(response.voices()).singleElement().satisfies(result -> {
            assertThat(result.previewAudioUrl()).isNull();
            assertThat(result.previewAudioStatus()).isEqualTo(TtsAudioStatus.PENDING);
            assertThat(result.doneAudioUrl()).isNull();
            assertThat(result.doneAudioStatus()).isEqualTo(TtsAudioStatus.PENDING);
            assertThat(result.remindAudioUrl()).isNull();
            assertThat(result.remindAudioStatus()).isEqualTo(TtsAudioStatus.PENDING);
        });
    }

    @Test
    void returnsNullPreviewUrlWhenBaseUrlIsMissing() {
        TTS voice = createVoice(
                1L,
                "Leda",
                "tts/previews/v1/leda.mp3",
                "tts/common/v1/leda-done.mp3",
                "tts/common/v1/leda-remind.mp3",
                1
        );

        TTSResponseDTO.VoiceListResponse response = TTSConverter.toVoiceListResponse(
                List.of(voice),
                ""
        );

        assertThat(response.voices()).singleElement().satisfies(result -> {
            assertThat(result.previewAudioUrl()).isNull();
            assertThat(result.doneAudioUrl()).isNull();
            assertThat(result.remindAudioUrl()).isNull();
            assertThat(result.previewAudioStatus()).isEqualTo(TtsAudioStatus.READY);
            assertThat(result.doneAudioStatus()).isEqualTo(TtsAudioStatus.READY);
            assertThat(result.remindAudioStatus()).isEqualTo(TtsAudioStatus.READY);
        });
    }

    private TTS createVoice(
            Long id,
            String name,
            String previewAudioKey,
            String doneAudioKey,
            String remindAudioKey,
            Integer selectionVersion
    ) {
        return TTS.builder()
                .id(id)
                .name(name)
                .label(name)
                .description("미리듣기 테스트 음성")
                .previewAudioKey(previewAudioKey)
                .doneAudioKey(doneAudioKey)
                .remindAudioKey(remindAudioKey)
                .selectionVersion(selectionVersion)
                .isProOnly(false)
                .build();
    }
}
