package com.shopflow.order.query;

import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCompletedEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.Topics;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.domain.OrderSummaryEntity;
import com.shopflow.order.repo.OrderSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS PROJECTION - the bridge from write side to read side.
 *
 * Subscribes to the ORDER_EVENTS stream and folds each event into the flat
 * read model. This is the ONLY writer of order_summaries.
 *
 * Note the consumer group "order-projection" is DIFFERENT from the saga's
 * group: in Kafka, each group gets its own full copy of the stream, so the
 * projection and any other consumer (e.g. notifications) process the same
 * events independently - that's the fan-out that makes event-driven read
 * models cheap to add.
 *
 * Handlers are written to be IDEMPOTENT (Kafka is at-least-once: redelivery
 * after a consumer crash is normal, so "handle the same event twice" must be
 * harmless - here, saving the same snapshot again).
 */
@Component
// Class-level listener + @KafkaHandler methods route each event type to its own
// method (see InventoryCommandListener for why a bare Object parameter fails).
@KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "order-projection")
public class OrderProjection {

    private static final Logger log = LoggerFactory.getLogger(OrderProjection.class);

    private final OrderSummaryRepository summaryRepository;

    public OrderProjection(OrderSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    /** A new order appears in the read model. */
    @KafkaHandler
    @Transactional
    public void onOrderCreated(OrderCreatedEvent created) {
        // Upsert (idempotent): redelivery just overwrites the same row.
        summaryRepository.save(new OrderSummaryEntity(
                created.orderId(), created.userId(), created.productId(),
                created.quantity(), created.totalAmount(),
                OrderStatus.PENDING, created.occurredAt()));
        log.debug("Projected OrderCreated -> summary {}", created.orderId());
    }

    /** Saga finished successfully. */
    @KafkaHandler
    @Transactional
    public void onOrderCompleted(OrderCompletedEvent completed) {
        summaryRepository.findById(completed.orderId()).ifPresent(s -> {
            s.updateStatus(OrderStatus.COMPLETED, "fulfilled");
            summaryRepository.save(s);
        });
    }

    /** Saga failed or timed out; the reason is surfaced to the customer. */
    @KafkaHandler
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent cancelled) {
        summaryRepository.findById(cancelled.orderId()).ifPresent(s -> {
            s.updateStatus(OrderStatus.CANCELLED, cancelled.reason());
            summaryRepository.save(s);
        });
    }

    /**
     * Event types this projection doesn't care about land here. Being tolerant
     * is deliberate - NEW event types added later must not break OLD consumers -
     * but we log at debug so the skip is discoverable rather than invisible.
     */
    @KafkaHandler(isDefault = true)
    public void onOther(Object payload) {
        log.debug("Projection ignoring event of type {}",
                payload == null ? "null" : payload.getClass().getSimpleName());
    }
}
