package com.moru.server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.apple-withdrawal")
public class AppleWithdrawalProperties {

    private boolean allowMissingCredential;
}
