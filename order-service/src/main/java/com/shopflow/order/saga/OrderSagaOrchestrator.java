package com.shopflow.order.saga;

import com.shopflow.common.events.InventoryRejectedEvent;
import com.shopflow.common.events.InventoryReservedEvent;
import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCompletedEvent;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.common.events.ProcessPaymentCommand;
import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.order.command.EventStoreService;
import com.shopflow.order.domain.OrderEntity;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Drives the order saga across inventory- and payment-service.
 *
 * Each step is a local transaction in its own service. If a later step fails,
 * earlier ones are undone with compensating commands instead of a rollback:
 *
 *   reserve stock -> charge payment -> COMPLETED
 *        |                |
 *     rejected         failed -> release stock -> CANCELLED
 */
@Component
// One listener over both reply topics; @KafkaHandler methods route by payload type.
@KafkaListener(topics = {Topics.INVENTORY_EVENTS, Topics.PAYMENT_EVENTS}, groupId = "order-saga")
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final EventStoreService eventStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                 EventStoreService eventStore,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ------------------------------------------------------------------
    // Saga step 1 reply: inventory-service
    // ------------------------------------------------------------------

    /** Stock reserved: move to AWAITING_PAYMENT and request the charge. */
    @KafkaHandler
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent reserved) {
        orderRepository.findById(reserved.orderId()).ifPresent(order -> {
            // A late or duplicate reply must not resurrect a cancelled order.
            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("Ignoring InventoryReserved for order {} in state {}",
                        order.getId(), order.getStatus());
                return;
            }
            order.transitionTo(OrderStatus.AWAITING_PAYMENT);
            kafkaTemplate.send(Topics.PAYMENT_COMMANDS, order.getId().toString(),
                    new ProcessPaymentCommand(order.getId(), order.getUserId(), order.getTotalAmount()));
            log.info("Saga[{}]: inventory reserved -> requesting payment", order.getId());
        });
    }

    /** Out of stock: nothing was reserved, so just cancel the order. */
    @KafkaHandler
    @Transactional
    public void onInventoryRejected(InventoryRejectedEvent rejected) {
        cancelOrder(rejected.orderId(), "Inventory rejected: " + rejected.reason());
    }

    // ------------------------------------------------------------------
    // Saga step 2 reply: payment-service
    // ------------------------------------------------------------------

    /** Payment captured: the order is complete. */
    @KafkaHandler
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent completed) {
        orderRepository.findById(completed.orderId()).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                log.warn("Ignoring PaymentCompleted for order {} in state {}",
                        order.getId(), order.getStatus());
                return;
            }
            order.transitionTo(OrderStatus.COMPLETED);
            eventStore.appendAndPublish(order.getId(),
                    new OrderCompletedEvent(order.getId(), order.getUserId(), Instant.now()));
            log.info("Saga[{}]: COMPLETED (payment ref {})", order.getId(), completed.paymentReference());
        });
    }

    /**
     * Payment failed after stock was reserved. The reservation is already
     * committed elsewhere, so it is undone with a compensating command.
     */
    @KafkaHandler
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent failed) {
        kafkaTemplate.send(Topics.INVENTORY_COMMANDS, failed.orderId().toString(),
                new ReleaseInventoryCommand(failed.orderId(), "payment failed"));
        cancelOrder(failed.orderId(), "Payment failed: " + failed.reason());
    }

    /** Unrecognized payloads are logged rather than dropped silently. */
    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.error("Unhandled saga reply of type {} - saga will stall. Payload: {}",
                payload == null ? "null" : payload.getClass().getName(), payload);
    }

    private void cancelOrder(java.util.UUID orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
                return;   // terminal states are final
            }
            order.transitionTo(OrderStatus.CANCELLED);
            eventStore.appendAndPublish(order.getId(),
                    new OrderCancelledEvent(order.getId(), order.getUserId(), reason, Instant.now()));
            log.info("Saga[{}]: CANCELLED ({})", orderId, reason);
        });
    }
}
