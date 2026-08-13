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
 * Builds the read model from the order event stream. Only writer of
 * order_summaries.
 *
 * Its consumer group is separate from the saga's, so both read the same events
 * independently. Handlers are idempotent because Kafka may redeliver.
 */
@Component
@KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "order-projection")
public class OrderProjection {

    private static final Logger log = LoggerFactory.getLogger(OrderProjection.class);

    private final OrderSummaryRepository summaryRepository;

    public OrderProjection(OrderSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    /** Adds the order to the read model. */
    @KafkaHandler
    @Transactional
    public void onOrderCreated(OrderCreatedEvent created) {
        // Upsert: redelivery just overwrites the same row.
        summaryRepository.save(new OrderSummaryEntity(
                created.orderId(), created.userId(), created.productId(),
                created.quantity(), created.totalAmount(),
                OrderStatus.PENDING, created.occurredAt()));
        log.debug("Projected OrderCreated -> summary {}", created.orderId());
    }

    /** Marks the order completed. */
    @KafkaHandler
    @Transactional
    public void onOrderCompleted(OrderCompletedEvent completed) {
        summaryRepository.findById(completed.orderId()).ifPresent(s -> {
            s.updateStatus(OrderStatus.COMPLETED, "fulfilled");
            summaryRepository.save(s);
        });
    }

    /** Marks the order cancelled and records why. */
    @KafkaHandler
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent cancelled) {
        summaryRepository.findById(cancelled.orderId()).ifPresent(s -> {
            s.updateStatus(OrderStatus.CANCELLED, cancelled.reason());
            summaryRepository.save(s);
        });
    }

    /**
     * Event types this projection doesn't handle. Ignoring them keeps older
     * consumers working when new event types are added.
     */
    @KafkaHandler(isDefault = true)
    public void onOther(Object payload) {
        log.debug("Projection ignoring event of type {}",
                payload == null ? "null" : payload.getClass().getSimpleName());
    }
}
