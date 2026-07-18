package com.moru.server.domain.tts.service.query;

import com.moru.server.domain.tts.dto.TTSResponseDTO;

public interface TTSQueryService {

    TTSResponseDTO.VoiceListResponse getVoices();
}
