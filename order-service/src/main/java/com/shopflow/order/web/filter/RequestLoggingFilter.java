package com.shopflow.order.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * SERVLET FILTER (jakarta.servlet.Filter) - the OUTERMOST interception layer.
 *
 * FILTER vs INTERCEPTOR (classic interview question) - see also TimingInterceptor:
 *  - Filters belong to the SERVLET SPEC, run in the container's filter chain
 *    BEFORE the request ever reaches Spring's DispatcherServlet, and see every
 *    request including static resources and 404s. They know nothing about
 *    which @Controller will handle the request.
 *  - Interceptors belong to SPRING MVC, run AFTER the DispatcherServlet has
 *    mapped the request to a handler, and can inspect that handler.
 *
 * Order of execution:  Filter.doFilter -> DispatcherServlet
 *                      -> Interceptor.preHandle -> Controller
 *                      -> Interceptor.postHandle / afterCompletion
 *                      -> back out through the filter
 *
 * This filter assigns a CORRELATION ID to each request and puts it in the
 * SLF4J MDC so every log line for this request carries the same id - the
 * poor man's distributed tracing (real systems propagate it cross-service
 * via headers; OpenTelemetry/Zipkin automate that).
 *
 * @Order(1): lowest value = outermost filter (runs first in, last out).
 */
@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Reuse the caller's correlation id if present (gateway / another service),
        // otherwise mint one - so one id follows the request across hops.
        String correlationId = req.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put("correlationId", correlationId);           // appears in every log line (see logback pattern)
        res.setHeader(CORRELATION_ID_HEADER, correlationId); // echoed back for client-side debugging

        long start = System.currentTimeMillis();
        try {
            log.info(">> {} {}", req.getMethod(), req.getRequestURI());
            chain.doFilter(request, response);             // hand off to the next filter / servlet
        } finally {
            log.info("<< {} {} -> {} ({} ms)", req.getMethod(), req.getRequestURI(),
                    res.getStatus(), System.currentTimeMillis() - start);
            MDC.clear();                                   // ALWAYS clear: threads are pooled and reused!
        }
    }
}
