package com.shopflow.order.query;

import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.domain.OrderSummaryEntity;
import com.shopflow.order.repo.OrderSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the read-model projection.
 *
 * Worth covering:
 *  - the same OrderCreatedEvent twice -> still one row (redelivery must be harmless)
 *  - OrderCompletedEvent -> existing row becomes COMPLETED
 *  - OrderCancelledEvent -> existing row becomes CANCELLED and records the reason
 *  - a completed/cancelled event for an unknown order id -> no exception, nothing saved
 */
@ExtendWith(MockitoExtension.class)
class OrderProjectionTest {

    @Mock
    private OrderSummaryRepository summaryRepository;

    @InjectMocks
    private OrderProjection projection;

    @Test
    void orderCreatedEventBecomesAPendingRowInTheReadModel() {
        // given
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-01-01T10:00:00Z");
        OrderCreatedEvent created = new OrderCreatedEvent(
                orderId, "user-1", "p-1001", 2, new BigDecimal("199.98"), occurredAt);

        // when
        projection.onOrderCreated(created);

        // then the read model mirrors the event, starting at PENDING
        ArgumentCaptor<OrderSummaryEntity> saved = ArgumentCaptor.forClass(OrderSummaryEntity.class);
        verify(summaryRepository).save(saved.capture());

        assertThat(saved.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(occurredAt);
    }
}
