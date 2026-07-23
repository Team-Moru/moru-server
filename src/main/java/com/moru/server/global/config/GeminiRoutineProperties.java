package com.moru.server.global.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini-routine")
public record GeminiRoutineProperties(
        String apiKey,
        String model
) {}
