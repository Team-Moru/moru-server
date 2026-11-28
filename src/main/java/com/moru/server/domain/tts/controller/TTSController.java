package com.moru.server.domain.tts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.moru.server.global.security.auth.AuthenticatedInstallation;
import com.moru.server.global.security.auth.AuthenticatedMember;
import com.moru.server.domain.tts.service.command.SingleTtsService;
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
    private final SingleTtsService singleTts;

    @PostMapping("/synthesize")
    public ApiResponse<TTSResponseDTO.SynthesisResponse> synthesize(
            @AuthenticationPrincipal AuthenticatedInstallation installation,
            @Valid @RequestBody SynthesisRequest request) {
        String url = singleTts.synthesize(installation.id(), request.ttsId(), request.text());
        return ApiResponse.onSuccess(new TTSResponseDTO.SynthesisResponse(url));
    }

    public record SynthesisRequest(@NotNull @Positive Long ttsId, @NotBlank String text) {
    }

    @Operation(summary = "TTS 목소리 목록 조회", description = "선택 가능한 TTS 목소리 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<TTSResponseDTO.VoiceListResponse> getVoices(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.onSuccess(ttsQueryService.getVoices(member != null));
    }
}
