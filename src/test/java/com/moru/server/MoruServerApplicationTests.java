package com.moru.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

@ActiveProfiles("test")
@SpringBootTest
class MoruServerApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoadsWithoutTtsConfiguration() {
		assertThat(applicationContext.getEnvironment().containsProperty("google.tts.enabled")).isFalse();
		assertThat(applicationContext.getBeansOfType(TextToSpeechClient.class)).isEmpty();
		assertThat(applicationContext.getBeansOfType(S3Client.class)).isEmpty();
	}

}
