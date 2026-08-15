package com.moru.server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.apple-token")
public class AppleTokenEncryptionProperties {

    private String encryptionKey = "";
}
