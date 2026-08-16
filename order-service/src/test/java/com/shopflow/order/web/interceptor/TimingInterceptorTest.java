package com.shopflow.order.web.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the timing interceptor.
 *
 * Worth covering:
 *  - afterCompletion with a HandlerMethod completes without error
 *  - afterCompletion when preHandle never ran (no start attribute) -> no exception
 *  - afterCompletion with a non-HandlerMethod handler -> ignored safely
 *
 * A HandlerMethod can be built from any object and method, e.g.
 *   new HandlerMethod(this, getClass().getDeclaredMethod("someMethod"));
 */
class TimingInterceptorTest {

    private final TimingInterceptor interceptor = new TimingInterceptor();

    @Test
    void preHandleRecordsAStartTimeAndLetsTheRequestContinue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        // returning false here would abort the request before the controller runs
        assertThat(proceed).isTrue();
        assertThat(request.getAttribute("timing.start")).isInstanceOf(Long.class);
    }
}
