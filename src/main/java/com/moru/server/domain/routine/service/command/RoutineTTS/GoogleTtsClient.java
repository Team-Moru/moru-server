package com.moru.server.domain.routine.service.command.RoutineTTS;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TTS 합성 <b>전용</b> 컴포넌트. 텍스트를 받아 오디오 바이트로만 바꾼다.
 * DB·S3·상태변경을 전혀 모른다(단일 책임). Google Cloud TTS SDK 를 감싼다.
 */
@Component
@RequiredArgsConstructor
public class GoogleTtsClient {

    private final TextToSpeechClient textToSpeechClient;

    @Value("${google.tts.language-code}")
    private String languageCode;

    @Value("${google.tts.voice}")
    private String voiceName;

    @Value("${google.tts.audio-encoding}")
    private String audioEncoding;

    /**
     * 텍스트 → 오디오 바이트(현재 설정상 MP3).
     *
     * <p>목소리는 지금은 설정값(google.tts.voice) 하나로 고정이다.
     * 회원별 목소리(Member.voiceType → Chirp 매핑)는 아직 미정이라,
     * 확정되면 이 메서드에 voice 파라미터를 추가하고 호출부에서 넘기면 된다.
     */
    public byte[] synthesize(String text) {
        SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
                SynthesisInput.newBuilder().setText(text).build(),
                VoiceSelectionParams.newBuilder()
                        .setLanguageCode(languageCode)
                        .setName(voiceName)
                        .build(),
                AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.valueOf(audioEncoding))
                        .build());

        return response.getAudioContent().toByteArray();
    }
}
