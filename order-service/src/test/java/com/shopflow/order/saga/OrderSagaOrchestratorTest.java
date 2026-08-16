package com.shopflow.order.saga;

import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.PaymentFailedEvent;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the saga state machine - the most valuable tests in this module.
 *
 * Worth covering:
 *  - inventory reserved on a PENDING order   -> AWAITING_PAYMENT + ProcessPaymentCommand sent
 *  - inventory reserved on a CANCELLED order -> ignored (no state change, no command)
 *  - inventory rejected                      -> CANCELLED + OrderCancelledEvent, no compensation
 *  - payment completed on AWAITING_PAYMENT   -> COMPLETED + OrderCompletedEvent
 *  - payment completed on a PENDING order    -> ignored (out-of-order reply)
 *  - cancelling an already COMPLETED order   -> no-op (terminal state)
 *  - an unknown payload type                 -> logged, nothing else happens
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventStoreService eventStore;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderSagaOrchestrator orchestrator;

    /** Convenience factory for an order in its initial PENDING state. */
    private static OrderEntity pendingOrder(UUID orderId) {
        return new OrderEntity(orderId, "user-1", "p-1001", 2, new BigDecimal("199.98"));
    }

    @Test
    void paymentFailureReleasesReservedStockAndCancelsTheOrder() {
        // given an order that already reserved stock and is waiting on payment
        UUID orderId = UUID.randomUUID();
        OrderEntity order = pendingOrder(orderId);
        order.transitionTo(OrderStatus.AWAITING_PAYMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when payment fails
        orchestrator.onPaymentFailed(new PaymentFailedEvent(orderId, "card declined"));

        // then the earlier step is compensated - the reservation is explicitly undone
        verify(kafkaTemplate).send(
                eq(Topics.INVENTORY_COMMANDS),
                eq(orderId.toString()),
                any(ReleaseInventoryCommand.class));

        // and the order ends cancelled, with the fact recorded
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventStore).appendAndPublish(any(OrderCancelledEvent.class));
    }
}
