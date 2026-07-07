package com.moru.server.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;
import com.moru.server.global.response.ApiResponse;

@Tag(name = "Dev Auth", description = "개발용 인증 API")
@Profile({"dev", "local"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/auth")
public class DevAuthController {

    private final AuthCommandService authCommandService;

    @Operation(summary = "임시 토큰 발급", description = "소셜 로그인 구현 전까지 사용할 개발용 토큰을 발급합니다.")
    @PostMapping("/token")
    public ApiResponse<AuthResponseDTO.TokenResponse> issueDevToken(
            @Valid @RequestBody AuthRequestDTO.DevTokenRequest request
    ) {
        return ApiResponse.onSuccess(authCommandService.issueDevToken(request));
    }
}
