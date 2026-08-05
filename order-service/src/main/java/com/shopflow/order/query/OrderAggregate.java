package com.shopflow.order.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCompletedEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.order.domain.OrderEventEntity;
import com.shopflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EVENT SOURCING demonstrated end-to-end: current state as a pure function of
 * the event history. No table is read - only events are folded, one by one,
 * through apply(). This is exactly how frameworks like Axon rebuild aggregates.
 */
public class OrderAggregate {

    private UUID orderId;
    private String userId;
    private String productId;
    private int quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String cancellationReason;
    private final List<String> appliedEvents = new ArrayList<>();   // visible proof of the replay

    public static OrderAggregate replay(List<OrderEventEntity> history, ObjectMapper mapper) {
        OrderAggregate agg = new OrderAggregate();
        for (OrderEventEntity stored : history) {
            agg.apply(deserialize(stored, mapper));
            agg.appliedEvents.add(stored.getSeqNo() + ":" + stored.getEventType());
        }
        return agg;
    }

    /** One state transition per event type - the fold step. */
    private void apply(Object event) {
        if (event instanceof OrderCreatedEvent e) {
            this.orderId = e.orderId();
            this.userId = e.userId();
            this.productId = e.productId();
            this.quantity = e.quantity();
            this.totalAmount = e.totalAmount();
            this.status = OrderStatus.PENDING;
        } else if (event instanceof OrderCompletedEvent) {
            this.status = OrderStatus.COMPLETED;
        } else if (event instanceof OrderCancelledEvent e) {
            this.status = OrderStatus.CANCELLED;
            this.cancellationReason = e.reason();
        }
        // Unknown historical event types: skipped (forward compatibility).
    }

    private static Object deserialize(OrderEventEntity stored, ObjectMapper mapper) {
        try {
            Class<?> type = Class.forName("com.shopflow.common.events." + stored.getEventType());
            return mapper.readValue(stored.getPayload(), type);
        } catch (Exception e) {
            return stored;   // unknown type - apply() will ignore it
        }
    }

    // Getters (serialized into the /rebuilt response)
    public UUID getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public List<String> getAppliedEvents() { return appliedEvents; }
}
