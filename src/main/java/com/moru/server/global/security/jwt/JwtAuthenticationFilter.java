package com.moru.server.global.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import com.moru.server.global.security.auth.AuthenticatedInstallation;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.response.code.BaseCode;
import com.moru.server.global.response.code.status.ErrorStatus;
import com.moru.server.global.security.auth.AuthenticatedMember;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TOKEN_REISSUE_PATH = "/auth/reissue";
    private static final PathPatternRequestMatcher PUBLIC_TTS =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/tts");

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return TOKEN_REISSUE_PATH.equals(path)
                || ("POST".equals(request.getMethod())
                    && ("/auth/anonymous".equals(path) || "/auth/anonymous/refresh".equals(path)));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String accessToken = resolveAccessToken(request);
            if (StringUtils.hasText(accessToken)) {
                setAuthentication(request, accessToken);
            }
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            if (!PUBLIC_TTS.matches(request)) {
                writeErrorResponse(response, e.getBaseCode());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorStatus.MALFORMED_TOKEN);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorStatus.TOKEN_MISSING);
        }

        return token;
    }

    private void setAuthentication(HttpServletRequest request, String accessToken) {
        Object principal = jwtTokenProvider.getAccessPrincipal(accessToken);
        String authority = principal instanceof AuthenticatedMember member
                ? ROLE_PREFIX + member.role().name()
                : AuthenticatedInstallation.AUTHORITY;
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeErrorResponse(HttpServletResponse response, BaseCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.onFailure(errorCode)));
    }
}
