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
 * Turns order events into customer notifications.
 *
 * Its own consumer group means it reads the order event stream independently
 * of the other services consuming the same topic.
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
        // Cancellations are urgent, so they also go out by SMS.
        dispatcher.dispatch(cancelled.userId(), "Order cancelled",
                "Order " + cancelled.orderId() + " was cancelled: " + cancelled.reason(),
                true);
    }

    /** Other event types don't produce notifications. */
    @KafkaHandler(isDefault = true)
    public void onOther(Object payload) {
        // no-op by design
    }
}
