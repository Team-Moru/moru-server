package com.moru.server.domain.tts.service.query;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moru.server.domain.tts.converter.TTSConverter;
import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TTSQueryServiceImpl implements TTSQueryService {

    private final TTSRepository ttsRepository;

    @Override
    public TTSResponseDTO.VoiceListResponse getVoices() {
        List<TTS> voices = ttsRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        return TTSConverter.toVoiceListResponse(voices);
    }
}
