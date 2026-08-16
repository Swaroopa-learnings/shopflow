package com.shopflow.order.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.events.OrderCompletedEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.order.domain.OrderEventEntity;
import com.shopflow.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for rebuilding order state by replaying stored events.
 *
 * No mocks needed - this is pure logic over a list of events, which makes it a
 * good place to start.
 *
 * The wider point: once there is an integration test that runs a real order
 * through the saga, assert that replaying its events produces the same status
 * the read model reports. That is the invariant this class exists to check.
 *
 * Worth covering:
 *  - replaying [OrderCreated] alone -> PENDING
 *  - replaying [OrderCreated, OrderCancelled] -> CANCELLED with the reason
 *  - an unknown eventType in the stream is skipped rather than throwing
 *    (this is what lets old consumers survive new event types)
 */
class OrderAggregateTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /** Turns a real event object into the row shape the event store holds. */
    private OrderEventEntity stored(UUID orderId, int seqNo, Object event) throws Exception {
        return new OrderEventEntity(
                orderId, seqNo, event.getClass().getSimpleName(), objectMapper.writeValueAsString(event));
    }

    @Test
    void stateIsDerivedEntirelyFromTheEventHistory() throws Exception {
        // given an order that was created and then completed
        UUID orderId = UUID.randomUUID();
        List<OrderEventEntity> history = List.of(
                stored(orderId, 1, new OrderCreatedEvent(
                        orderId, "user-1", "p-1001", 2, new BigDecimal("199.98"), Instant.now())),
                stored(orderId, 2, new OrderCompletedEvent(orderId, "user-1", Instant.now())));

        // when the history is folded back into state - no table is read
        OrderAggregate aggregate = OrderAggregate.replay(history, objectMapper);

        // then
        assertThat(aggregate.getOrderId()).isEqualTo(orderId);
        assertThat(aggregate.getProductId()).isEqualTo("p-1001");
        assertThat(aggregate.getQuantity()).isEqualTo(2);
        assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(aggregate.getAppliedEvents())
                .containsExactly("1:OrderCreatedEvent", "2:OrderCompletedEvent");
    }
}
