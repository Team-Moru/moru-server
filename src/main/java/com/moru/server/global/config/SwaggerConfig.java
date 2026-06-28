package com.moru.server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI moruOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Moru Server API")
				.description("Moru server API documentation")
				.version("v1"));
	}

}
