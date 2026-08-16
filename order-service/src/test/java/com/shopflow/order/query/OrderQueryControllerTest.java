package com.shopflow.order.query;

import com.shopflow.order.repo.OrderEventRepository;
import com.shopflow.order.repo.OrderSummaryRepository;
import com.shopflow.order.security.JwtAuthFilter;
import com.shopflow.order.web.interceptor.TimingInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the order read API.
 *
 * Worth covering:
 *  - GET /{id} when the summary exists -> 200 with the expected fields
 *  - GET / returns only the caller's orders (the repository is queried with the
 *    id from X-User-Id, not with anything from the query string)
 *  - GET /{id}/events  -> events in sequence order, payload parsed as JSON
 *  - GET /{id}/events for an unknown order -> empty list
 *  - a malformed UUID in the path -> 400
 */
@WebMvcTest(OrderQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderSummaryRepository summaryRepository;

    @MockBean
    private OrderEventRepository eventRepository;

    // @WebMvcTest picks up WebMvcConfigurer, SecurityFilterChain and Filter beans,
    // so their dependencies must exist even though this slice never exercises them.
    // StringRedisTemplate is here because IdempotencyFilter is a Filter @Component.
    @MockBean
    private TimingInterceptor timingInterceptor;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void letRequestsReachTheController() throws Exception {
        // A mocked HandlerInterceptor returns false from preHandle by default,
        // which aborts the request before the controller runs - every response
        // then comes back as an empty 200. Stub it to true.
        when(timingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(summaryRepository.findById(orderId)).thenReturn(Optional.empty());

        // an id that is not in the read model yet looks exactly like one that
        // never existed - the read side cannot tell the difference
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNotFound());
    }
}
