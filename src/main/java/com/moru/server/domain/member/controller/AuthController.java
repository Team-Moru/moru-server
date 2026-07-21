package com.moru.server.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.enums.OAuthProvider;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;
import com.moru.server.global.response.ApiResponse;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthCommandService authCommandService;

    @Operation(summary = "소셜 로그인", description = "소셜 토큰을 검증한 뒤 회원가입 또는 로그인을 처리합니다.")
    @PostMapping("/login/{provider}")
    public ApiResponse<AuthResponseDTO.SocialLoginResponse> loginWithSocial(
            @PathVariable String provider,
            @Valid @RequestBody AuthRequestDTO.SocialLoginRequest request
    ) {
        return ApiResponse.onSuccess(authCommandService.loginWithSocial(OAuthProvider.from(provider), request));
    }
}
