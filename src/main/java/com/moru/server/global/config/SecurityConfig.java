package com.moru.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import com.moru.server.global.security.auth.AuthenticatedInstallation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import com.moru.server.global.security.auth.DevAuthSecretFilter;
import com.moru.server.global.security.jwt.JwtAuthenticationEntryPoint;
import com.moru.server.global.security.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            Environment environment,
            ObjectMapper objectMapper,
            @Value("${dev-auth.secret:}") String devAuthSecret
    ) throws Exception {
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        DevAuthSecretFilter devAuthSecretFilter = new DevAuthSecretFilter(
                production,
                devAuthSecret,
                objectMapper
        );

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/auth/reissue",
                                "/auth/login/**",
                                "/dev/auth/token"
                        ).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                            .access((authentication, request) -> new AuthorizationDecision(!production))
                        .requestMatchers(HttpMethod.POST, "/auth/anonymous", "/auth/anonymous/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/routine-groups/ai-generate")
                            .hasAnyAuthority("ROLE_MEMBER", "ROLE_ADMIN", AuthenticatedInstallation.AUTHORITY)
                        .requestMatchers(HttpMethod.POST, "/tts/synthesize")
                            .hasAuthority(AuthenticatedInstallation.AUTHORITY)
                        .anyRequest().hasAnyRole("MEMBER", "ADMIN")
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(devAuthSecretFilter, JwtAuthenticationFilter.class)
                .build();
    }
}
