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
 * Servlet filter that gives every request a correlation id and logs its
 * method, path, status and duration.
 *
 * The id goes into the SLF4J MDC so all log lines for one request can be
 * grepped together, and is echoed back in the response header.
 * Runs outermost (@Order(1)), before Spring MVC sees the request.
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

        // Reuse the caller's id if present so it follows the request across services.
        String correlationId = req.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put("correlationId", correlationId);
        res.setHeader(CORRELATION_ID_HEADER, correlationId);

        long start = System.currentTimeMillis();
        try {
            log.info(">> {} {}", req.getMethod(), req.getRequestURI());
            chain.doFilter(request, response);
        } finally {
            log.info("<< {} {} -> {} ({} ms)", req.getMethod(), req.getRequestURI(),
                    res.getStatus(), System.currentTimeMillis() - start);
            MDC.clear();   // must clear - threads are pooled and reused
        }
    }
}
