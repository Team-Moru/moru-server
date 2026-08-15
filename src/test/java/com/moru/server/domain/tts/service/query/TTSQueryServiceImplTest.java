package com.moru.server.domain.tts.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;
import com.moru.server.global.config.AssetProperties;

@ExtendWith(MockitoExtension.class)
class TTSQueryServiceImplTest {

    @Mock
    private TTSRepository ttsRepository;

    private TTSQueryServiceImpl ttsQueryService;

    @BeforeEach
    void setUp() {
        AssetProperties assetProperties = new AssetProperties();
        assetProperties.setPublicBaseUrl("https://assets.example.com");
        ttsQueryService = new TTSQueryServiceImpl(ttsRepository, assetProperties);
    }

    @Test
    void returnsVoicesInRepositoryOrderWithPreviewUrls() {
        TTS leda = createVoice(1L, "Leda", "tts/previews/v1/leda.mp3");
        TTS kore = createVoice(2L, "Kore", "tts/previews/v1/kore.mp3");
        when(ttsRepository.findAllByOrderByIdAsc()).thenReturn(List.of(leda, kore));

        TTSResponseDTO.VoiceListResponse response = ttsQueryService.getVoices();

        assertThat(response.voices())
                .extracting(TTSResponseDTO.VoiceResponse::ttsId)
                .containsExactly(1L, 2L);
        assertThat(response.voices())
                .extracting(TTSResponseDTO.VoiceResponse::previewAudioUrl)
                .containsExactly(
                        "https://assets.example.com/tts/previews/v1/leda.mp3",
                        "https://assets.example.com/tts/previews/v1/kore.mp3"
                );
        verify(ttsRepository).findAllByOrderByIdAsc();
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
