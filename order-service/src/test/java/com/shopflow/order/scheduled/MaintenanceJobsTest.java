package com.shopflow.order.scheduled;

import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.order.command.EventStoreService;
import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.repo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the scheduled jobs - the timeout sweep is the one that matters,
 * because without it a stalled saga would never resolve.
 *
 * Worth covering:
 *  - an AWAITING_PAYMENT order past the cutoff -> also swept, not just PENDING
 *  - nothing stale -> no events, no Kafka messages
 *  - the cancellation reason is recorded as "saga timeout"
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceJobsTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventStoreService eventStore;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MaintenanceJobs maintenanceJobs;

    @Test
    void stalePendingOrderIsCancelledAndItsStockReleased() {
        // given one order stuck in PENDING past the cutoff
        UUID orderId = UUID.randomUUID();
        OrderEntity stale = new OrderEntity(orderId, "user-1", "p-1001", 1, new BigDecimal("10.00"));

        // NOTE: the job calls addAll() on the first list, so it must be mutable -
        // List.of(...) alone would throw UnsupportedOperationException here.
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), any(Instant.class)))
                .thenReturn(new ArrayList<>(List.of(stale)));
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.AWAITING_PAYMENT), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        // when
        maintenanceJobs.cancelStaleOrders();

        // then any stock it might hold is released ...
        verify(kafkaTemplate).send(
                eq(Topics.INVENTORY_COMMANDS),
                eq(orderId.toString()),
                any(ReleaseInventoryCommand.class));

        // ... and the order reaches a terminal state instead of waiting forever
        assertThat(stale.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventStore).appendAndPublish(any(OrderCancelledEvent.class));
    }
}
