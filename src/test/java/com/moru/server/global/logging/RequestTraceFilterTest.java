package com.moru.server.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reusesValidRequestIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "ios-request_123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) -> {
            requestIdInChain.set(MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY));
            ((MockHttpServletResponse) servletResponse).setStatus(201);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER)).isEqualTo("ios-request_123");
        assertThat(requestIdInChain).hasValue("ios-request_123");
        assertThat(MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "unsafe request\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER))
                .isNotBlank()
                .doesNotContain("unsafe", "\n")
                .hasSize(36);
    }

    @Test
    void restoresExistingMdcValue() throws Exception {
        MDC.put(RequestTraceFilter.REQUEST_ID_MDC_KEY, "parent-request");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY)).isEqualTo("parent-request");
    }
}
