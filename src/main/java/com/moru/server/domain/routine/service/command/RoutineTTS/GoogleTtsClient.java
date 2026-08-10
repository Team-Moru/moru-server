package com.moru.server.domain.routine.service.command.RoutineTTS;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "google.tts.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleTtsClient {

    private final TextToSpeechClient textToSpeechClient;

    @Value("${google.tts.language-code}")
    private String languageCode;

    @Value("${google.tts.audio-encoding}")
    private String audioEncoding;

    @Value("${google.tts.voice}")
    private String defaultVoice;

    public byte[] synthesize(String text,String voiceName) {

        String name = (voiceName == null || voiceName.isBlank())
                ? defaultVoice
                : "ko-KR-Chirp3-HD-" + voiceName;

        SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
                SynthesisInput.newBuilder().setText(text).build(),
                VoiceSelectionParams.newBuilder()
                        .setLanguageCode(languageCode)
                        .setName(name)
                        .build(),
                AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.valueOf(audioEncoding))
                        .build());

        return response.getAudioContent().toByteArray();
    }
}
