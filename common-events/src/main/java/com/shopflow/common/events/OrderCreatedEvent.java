package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A customer placed an order. First event in the order's stream and the
 * trigger for the saga.
 *
 * Unknown fields are ignored on read, so a newer producer can add optional
 * fields without breaking older consumers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        UUID orderId,

        String userId,
        String productId,
        int quantity,
        BigDecimal totalAmount,
        Instant occurredAt
) implements OrderEvent {
}
