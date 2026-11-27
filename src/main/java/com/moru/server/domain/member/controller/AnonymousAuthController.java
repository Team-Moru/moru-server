package com.moru.server.domain.member.controller;

import java.util.UUID;

import com.google.common.net.InetAddresses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;

import com.moru.server.domain.member.service.command.auth.AnonymousAuthService;
import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.response.ApiResponse;

@RestController
@RequestMapping("/auth/anonymous")
@RequiredArgsConstructor
public class AnonymousAuthController {
    private final AnonymousAuthService auth;
    private final V2UsageProperties properties;

    @PostMapping
    public ApiResponse<AnonymousAuthService.Tokens> register(@Valid @RequestBody Registration request,
                                                             HttpServletRequest httpRequest) {
        String proxyIp = properties.isTrustProxyClientIp() ? httpRequest.getHeader("X-Moru-Client-Ip") : null;
        String ip = proxyIp != null && InetAddresses.isInetAddress(proxyIp)
                ? InetAddresses.toAddrString(InetAddresses.forString(proxyIp))
                : httpRequest.getRemoteAddr();
        return ApiResponse.onSuccess(auth.register(request.installationId(), DigestUtils.sha256Hex(ip)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AnonymousAuthService.Tokens> refresh(@Valid @RequestBody Refresh request) {
        return ApiResponse.onSuccess(auth.refresh(request.refreshToken()));
    }

    public record Registration(@NotNull UUID installationId) {
    }

    public record Refresh(@NotBlank @Size(max = 2048) String refreshToken) {
    }
}
