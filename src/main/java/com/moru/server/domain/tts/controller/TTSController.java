package com.moru.server.domain.tts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moru.server.domain.tts.dto.TTSResponseDTO;
import com.moru.server.domain.tts.service.query.TTSQueryService;
import com.moru.server.global.response.ApiResponse;

@Tag(name = "TTS", description = "TTS API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/tts")
public class TTSController {

    private final TTSQueryService ttsQueryService;

    @Operation(summary = "TTS 목소리 목록 조회", description = "선택 가능한 TTS 목소리 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<TTSResponseDTO.VoiceListResponse> getVoices() {
        return ApiResponse.onSuccess(ttsQueryService.getVoices());
    }
}
