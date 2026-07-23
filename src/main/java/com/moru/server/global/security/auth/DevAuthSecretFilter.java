package com.moru.server.global.security.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.ObjectMapper;

import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.response.code.status.ErrorStatus;

public class DevAuthSecretFilter extends OncePerRequestFilter {

    public static final String SECRET_HEADER = "X-Dev-Auth-Secret";
    private static final String DEV_AUTH_TOKEN_PATH = "/dev/auth/token";

    private final boolean enabled;
    private final byte[] expectedSecret;
    private final ObjectMapper objectMapper;

    public DevAuthSecretFilter(boolean enabled, String expectedSecret, ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.expectedSecret = StringUtils.hasText(expectedSecret)
                ? expectedSecret.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return !enabled || !DEV_AUTH_TOKEN_PATH.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedSecret = request.getHeader(SECRET_HEADER);

        if (!matchesExpectedSecret(providedSecret)) {
            writeForbiddenResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matchesExpectedSecret(String providedSecret) {
        if (expectedSecret.length == 0 || !StringUtils.hasText(providedSecret)) {
            return false;
        }

        byte[] providedSecretBytes = providedSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedSecret, providedSecretBytes);
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorStatus.FORBIDDEN.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.onFailure(ErrorStatus.FORBIDDEN)));
    }
}
