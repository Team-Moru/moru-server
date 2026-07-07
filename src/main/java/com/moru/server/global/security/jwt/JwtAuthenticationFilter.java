package com.moru.server.global.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moru.server.domain.member.entity.enums.Role;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String accessToken = resolveAccessToken(request);

            if (!StringUtils.hasText(accessToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            jwtTokenProvider.validateAccessToken(accessToken);
            setAuthentication(request, accessToken);
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, e.getBaseCode());
        }
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
        Long memberId = jwtTokenProvider.getMemberId(accessToken);
        Role role = jwtTokenProvider.getRole(accessToken);
        AuthenticatedMember principal = new AuthenticatedMember(memberId, role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
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
