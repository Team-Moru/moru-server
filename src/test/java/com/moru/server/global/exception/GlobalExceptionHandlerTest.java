package com.moru.server.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.response.code.status.ErrorStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void doesNotExposeIllegalArgumentMessage() {
        ResponseEntity<Object> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("private user input")
        );

        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorStatus.BAD_REQUEST.getMessage());
        assertThat(body.getResult()).isNull();
    }

    @Test
    void doesNotExposeInternalExceptionMessage() {
        ResponseEntity<Object> response = handler.handleBeanCreationException(
                new BeanCreationException("database password was invalid")
        );

        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage());
        assertThat(body.getResult()).isNull();
    }

    @Test
    void returnsGenericResponseForUnhandledException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAllException(
                new RuntimeException("raw external API response")
        );

        ApiResponse<Void> body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage());
        assertThat(body.getResult()).isNull();
    }

    @Test
    void logsTheSameErrorCodeAndStatusAsTheBadRequestResponse() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            handler.handleDataIntegrityViolationException(
                    new DataIntegrityViolationException("private database value")
            );
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("category=data_integrity");
                    assertThat(message).contains("code=COMMON400");
                    assertThat(message).contains("status=400");
                    assertThat(message).doesNotContain("COMMON500", "private database value");
                });
    }
}
