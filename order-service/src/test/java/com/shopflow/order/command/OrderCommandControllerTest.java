package com.shopflow.order.command;

import com.shopflow.order.security.JwtAuthFilter;
import com.shopflow.order.web.dto.CreateOrderRequest;
import com.shopflow.order.web.interceptor.TimingInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the order write API. Only the MVC slice is loaded, so the
 * service is mocked and no database, Kafka or Redis is involved.
 *
 * addFilters = false skips the security chain, keeping these tests about
 * request mapping, validation and status codes.
 *
 * Worth covering:
 *  - quantity 0 or negative -> 400, with the field named in the response
 *  - quantity above the maximum -> 400
 *  - blank productId -> 400
 *  - an unsupported currency ("GBP") -> 400
 *  - missing X-User-Id header -> 400
 */
@WebMvcTest(OrderCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderCommandService commandService;

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
    void validRequestIsAcceptedAndReturnsThePendingOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(commandService.createOrder(eq("user-1"), any(CreateOrderRequest.class))).thenReturn(orderId);

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "user-1")          // set by the gateway from the JWT
                        .header("Idempotency-Key", "demo-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"p-1002","quantity":1,"currency":"USD"}
                                """))
                // 202, not 201: the order is recorded but fulfilment has not finished
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
