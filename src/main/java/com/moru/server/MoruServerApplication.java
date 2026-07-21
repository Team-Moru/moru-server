package com.moru.server;

import com.moru.server.global.config.GeminiRoutineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GeminiRoutineProperties.class)
public class MoruServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoruServerApplication.class, args);
	}

}
