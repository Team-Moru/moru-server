package com.moru.server.global.config;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "moru.v2")
@Validated
@Getter
@Setter
public class V2UsageProperties {
    @Positive private int registrationPerMinute = 30;
    @Positive private int refreshPerMinute = 5;
    @Positive private int aiPerMinute = 2;
    @Positive private int aiPerDay = 5;
    @Positive private int aiGlobalPerMinute = 10;
    @Positive private int aiGlobalPerDay = 300;
    @Positive private int ttsPerMinute = 10;
    @Positive private int ttsPerDay = 30;
    @Positive private int ttsGlobalPerMinute = 30;
    @Positive private int ttsGlobalPerDay = 300;
    @Positive private int ttsMaxCharacters = 250;
    private boolean trustProxyClientIp;
}
