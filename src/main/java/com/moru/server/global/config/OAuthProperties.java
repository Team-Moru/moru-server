package com.moru.server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Google google = new Google();
    private Apple apple = new Apple();

    @Getter
    @Setter
    public static class Google {

        private String clientId = "";
        private String issuer = "https://accounts.google.com";
        private String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";

        public boolean hasClientId() {
            return StringUtils.hasText(clientId);
        }
    }

    @Getter
    @Setter
    public static class Apple {

        private String issuer = "https://appleid.apple.com";
        private String jwksUri = "https://appleid.apple.com/auth/keys";
        private String audience = "";

        public boolean hasAudience() {
            return StringUtils.hasText(audience);
        }
    }
}
