package com.moru.server.global.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GoogleTtsConfig {

    private final ResourceLoader resourceLoader;

    public GoogleTtsConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public TextToSpeechClient textToSpeechClient(
            @Value("${google.tts.credentials-path}") String credentialsPath) throws IOException {

        //json 파일 읽어오기
        Resource resource = resourceLoader.getResource(credentialsPath);
        GoogleCredentials credentials;

        //Cloud TTS는 API키를 받지 않고 Oauth2 만 받습니다.
        try (InputStream in = resource.getInputStream()) {
            credentials = GoogleCredentials.fromStream(in);
        }


        TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();


        return TextToSpeechClient.create(settings);
    }


}
