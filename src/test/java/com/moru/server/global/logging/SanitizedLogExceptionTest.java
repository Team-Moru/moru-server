package com.moru.server.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class SanitizedLogExceptionTest {

    @Test
    void keepsCauseTypesWithoutOriginalMessagesOrStackFrames() {
        IllegalArgumentException cause = new IllegalArgumentException("private database value");
        IllegalStateException source = new IllegalStateException("raw external API response", cause);

        Throwable sanitized = SanitizedLogException.from(source);
        StringWriter output = new StringWriter();
        sanitized.printStackTrace(new PrintWriter(output));

        assertThat(sanitized.getStackTrace()).isEmpty();
        assertThat(sanitized.getCause()).isNotNull();
        assertThat(sanitized.getCause().getStackTrace()).isEmpty();
        assertThat(output.toString())
                .contains(IllegalStateException.class.getName())
                .contains(IllegalArgumentException.class.getName())
                .doesNotContain("SanitizedLogExceptionTest.java")
                .doesNotContain("raw external API response", "private database value");
    }
}
