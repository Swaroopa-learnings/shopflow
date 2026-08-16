package com.shopflow.order.web.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the idempotency filter.
 *
 * Redis is mocked, so for any test that reaches it, stub the indirection first:
 *   when(redis.opsForValue()).thenReturn(valueOps);
 * Otherwise opsForValue() returns null and you get an NPE rather than a
 * meaningful failure.
 *
 * Worth covering:
 *  - a GET request -> filter does not apply at all (shouldNotFilter)
 *  - first request with a key  -> setIfAbsent wins, handler runs, response stored
 *  - repeat with the same key  -> stored response replayed, handler NOT run,
 *                                 X-Idempotent-Replay header set
 *  - a key still marked in progress -> 409 Conflict, handler not run
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    void requestWithoutAnIdempotencyKeyIsPassedStraightThrough() throws Exception {
        IdempotencyFilter filter = new IdempotencyFilter(redis);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // the handler ran - MockFilterChain records what it was called with
        assertThat(chain.getRequest()).isSameAs(request);

        // and no key means no bookkeeping: Redis is never touched
        verifyNoInteractions(redis);
    }
}
