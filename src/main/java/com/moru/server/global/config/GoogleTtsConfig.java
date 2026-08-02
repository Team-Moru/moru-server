package com.moru.server.global.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "google.tts.enabled", havingValue = "true", matchIfMissing = true)
public class GoogleTtsConfig {

    private final ResourceLoader resourceLoader;

    public GoogleTtsConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public TextToSpeechClient textToSpeechClient(
            @Value("${google.tts.credentials-path}") String credentialsPath) throws IOException {

        Resource resource = resourceLoader.getResource(credentialsPath);
        GoogleCredentials credentials;

        try (InputStream in = resource.getInputStream()) {
            credentials = GoogleCredentials.fromStream(in);
        }


        TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();


        return TextToSpeechClient.create(settings);
    }


}
