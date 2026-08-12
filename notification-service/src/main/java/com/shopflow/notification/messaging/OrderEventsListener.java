package com.shopflow.notification.messaging;

import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCompletedEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.Topics;
import com.shopflow.notification.notify.NotificationDispatcher;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "notification-service" - its OWN copy of the ORDER_EVENTS
 * stream, independent of the saga and the CQRS projection reading the same
 * events. Kafka pub/sub fan-out in one line of config.
 */
@Component
@KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "notification-service")
public class OrderEventsListener {

    private final NotificationDispatcher dispatcher;

    public OrderEventsListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaHandler
    public void onOrderCreated(OrderCreatedEvent created) {
        dispatcher.dispatch(created.userId(), "Order received",
                "We received your order " + created.orderId() + " (total " + created.totalAmount() + ").",
                false);
    }

    @KafkaHandler
    public void onOrderCompleted(OrderCompletedEvent completed) {
        dispatcher.dispatch(completed.userId(), "Order confirmed",
                "Your order " + completed.orderId() + " is confirmed and being prepared!",
                false);
    }

    @KafkaHandler
    public void onOrderCancelled(OrderCancelledEvent cancelled) {
        // Cancellation is urgent -> email + SMS (the @Qualifier routing in action).
        dispatcher.dispatch(cancelled.userId(), "Order cancelled",
                "Order " + cancelled.orderId() + " was cancelled: " + cancelled.reason(),
                true);
    }

    /** Other event types simply aren't notification-worthy. */
    @KafkaHandler(isDefault = true)
    public void onOther(Object payload) {
        // no-op by design
    }
}
