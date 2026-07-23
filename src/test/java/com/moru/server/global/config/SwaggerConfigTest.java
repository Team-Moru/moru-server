package com.moru.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    @Test
    void usesCurrentOriginForSwaggerApiRequests() {
        OpenAPI openAPI = new SwaggerConfig().moruOpenApi();

        assertThat(openAPI.getServers())
                .extracting(Server::getUrl)
                .containsExactly("/");
    }
}
