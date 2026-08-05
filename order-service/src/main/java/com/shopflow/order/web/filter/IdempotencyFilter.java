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
 * IDEMPOTENCY - making "place order" safe to retry.
 *
 * THE PROBLEM: the client POSTs an order, the response times out. Did it
 * succeed? The client retries... and without protection buys everything twice.
 * Networks are unreliable, so EVERY money-adjacent POST needs this.
 *
 * THE PATTERN (as used by Stripe, PayPal, ...):
 *  1. Client generates a unique Idempotency-Key per logical operation and
 *     sends it as a header, REUSING the same key on retries.
 *  2. Server atomically records "key seen" (Redis SET NX = set-if-absent -
 *     one atomic operation, no read-then-write race between replicas).
 *  3. First request executes normally; its response is CACHED under the key.
 *  4. A retry with the same key gets the CACHED response replayed - the
 *     handler never runs twice. Marked with X-Idempotent-Replay: true.
 *  5. A retry arriving while the first is still executing gets 409 Conflict
 *     ("try again shortly") rather than racing it.
 *
 * Implemented as a OncePerRequestFilter (a Spring convenience Filter base
 * class guaranteed to run once per request even with forwards/includes).
 * Redis (not a local map) so it works across N replicas, with a TTL because
 * keys only need to outlive the client's retry window.
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
        // Only guard the mutating order endpoint; GETs are naturally idempotent.
        return !("POST".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/v1/orders"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            // Demo choice: header is optional. Payment APIs typically REQUIRE it (400 if absent).
            chain.doFilter(request, response);
            return;
        }

        String redisKey = "idem:" + key;

        // Atomic claim: true only for the FIRST request with this key.
        Boolean claimed = redis.opsForValue().setIfAbsent(redisKey, IN_PROGRESS, TTL);

        if (Boolean.FALSE.equals(claimed)) {
            String stored = redis.opsForValue().get(redisKey);
            if (IN_PROGRESS.equals(stored)) {
                // Original request still running - don't race it.
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"error\":\"Request with this Idempotency-Key is already in progress\"}");
                return;
            }
            // Completed before - REPLAY the stored response; handler never re-executes.
            log.info("Idempotent replay for key {}", key);
            int sep = stored.indexOf('|');
            response.setStatus(Integer.parseInt(stored.substring(0, sep)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-Idempotent-Replay", "true");
            response.getWriter().write(stored.substring(sep + 1));
            return;
        }

        // First time: execute, capturing the response body so we can store it.
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            redis.opsForValue().set(redisKey, wrapper.getStatus() + "|" + body, TTL);
        } finally {
            wrapper.copyBodyToResponse();   // actually send the captured body to the client
        }
    }
}
