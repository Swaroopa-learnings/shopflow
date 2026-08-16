package com.shopflow.order.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Makes order creation safe to retry, so a client that times out and resends
 * the request doesn't place a second order.
 *
 * The client sends an Idempotency-Key header. The first request runs and its
 * response is cached in Redis under that key; a retry replays the cached
 * response, and a retry arriving while the first is still running gets 409.
 * Redis (not a local map) so it works across replicas; entries expire by TTL.
 */
@Component
@Order(3)   // after logging(1); security runs in its own chain; before MVC
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String HEADER = "Idempotency-Key";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String IN_PROGRESS = "__IN_PROGRESS__";

    private final StringRedisTemplate redis;

    public IdempotencyFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only POSTs need guarding; GETs are already idempotent.
        log.info("In shouldNotFilter of IdempotencyFilter of order-service");
        return !("POST".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/v1/orders"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        log.info("In doFilterInternal of IdempotencyFilter of order-service");
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            // Header is optional here; payment APIs usually require it.
            chain.doFilter(request, response);
            return;
        }

        String redisKey = "idem:" + key;

        // SET NX: succeeds only for the first request with this key.
        Boolean claimed = redis.opsForValue().setIfAbsent(redisKey, IN_PROGRESS, TTL);

        if (Boolean.FALSE.equals(claimed)) {
            String stored = redis.opsForValue().get(redisKey);
            if (IN_PROGRESS.equals(stored)) {
                // First request still running - don't race it.
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"error\":\"Request with this Idempotency-Key is already in progress\"}");
                return;
            }
            // Already completed - replay the stored response.
            log.info("Idempotent replay for key {}", key);
            int sep = stored.indexOf('|');
            response.setStatus(Integer.parseInt(stored.substring(0, sep)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-Idempotent-Replay", "true");
            response.getWriter().write(stored.substring(sep + 1));
            return;
        }

        // First time: run it, capturing the response body so it can be stored.
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            redis.opsForValue().set(redisKey, wrapper.getStatus() + "|" + body, TTL);
        } finally {
            wrapper.copyBodyToResponse();   // send the captured body to the client
        }
    }
}
