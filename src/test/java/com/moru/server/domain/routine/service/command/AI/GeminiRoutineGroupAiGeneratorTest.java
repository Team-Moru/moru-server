package com.moru.server.domain.routine.service.command.AI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moru.server.domain.routine.entity.enums.RoutineType;
import com.moru.server.global.config.GeminiRoutineProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiRoutineGroupAiGeneratorTest {
    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "{\"candidates\":", "{\"candidates\":[]}"})
    void reportsMalformedSuccessfulResponsesAsParseFailures(String body) {
        assertFailure(generator(response(HttpStatus.OK, body)), ErrorStatus.AI_ROUTINE_PARSE_FAILED);
    }

    @Test
    void reportsProviderStatusAsUpstreamFailure() {
        assertFailure(generator(response(HttpStatus.TOO_MANY_REQUESTS, "{\"candidates\":")),
                ErrorStatus.AI_ROUTINE_UPSTREAM_UNAVAILABLE);
    }

    @Test
    void reportsTransportErrorAsUpstreamFailure() {
        var generator = generator((request, body, execution) -> {
            throw new IOException("connection closed");
        });

        assertFailure(generator, ErrorStatus.AI_ROUTINE_UPSTREAM_UNAVAILABLE);
    }

    @Test
    void parsesValidRoutineResponse() throws Exception {
        String routine = """
                {"title":"운동","description":"스트레칭하기","routines":[
                    {"title":"스트레칭","type":"TIMER","durationSecond":60}
                ]}
                """;
        String text = new ObjectMapper().writeValueAsString(routine);
        var generator = generator(response(HttpStatus.OK,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + text + "}]}}]}"));

        var result = generator.generate("스트레칭");

        assertThat(result.title()).isEqualTo("운동");
        assertThat(result.routines()).singleElement().satisfies(item -> {
            assertThat(item.type()).isEqualTo(RoutineType.TIMER);
            assertThat(item.durationSecond()).isEqualTo(60);
        });
    }

    private static GeminiRoutineGroupAiGenerator generator(ClientHttpRequestInterceptor interceptor) {
        return new GeminiRoutineGroupAiGenerator(new GeminiRoutineProperties("test-key", "test-model"),
                RestClient.builder().requestInterceptor(interceptor));
    }

    private static ClientHttpRequestInterceptor response(HttpStatus status, String body) {
        return (request, payload, execution) -> {
            var response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
    }

    private static void assertFailure(GeminiRoutineGroupAiGenerator generator, ErrorStatus error) {
        assertThatThrownBy(() -> generator.generate("스트레칭"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getBaseCode()).isEqualTo(error));
    }
}
