package com.shopflow.order.web.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the correlation-id filter. No dependencies, so no mocks.
 *
 * Worth covering:
 *  - an incoming X-Correlation-Id is reused unchanged (so it survives across services)
 *  - the MDC holds the id *during* the chain - assert from inside a custom FilterChain
 *  - the MDC is still cleared when the chain throws
 */
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void generatesACorrelationIdEchoesItBackAndClearsTheMdcAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // the caller gets an id back, which is what makes a report traceable
        assertThat(response.getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER)).isNotBlank();

        // and nothing leaks onto the thread - it will serve another request next
        assertThat(MDC.get("correlationId")).isNull();
    }
}
