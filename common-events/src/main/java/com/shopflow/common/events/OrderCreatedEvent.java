package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fact: a customer placed an order. First event in every order's event stream
 * and the trigger that starts the saga.
 *
 * BACKWARD COMPATIBILITY: {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * makes every consumer a "tolerant reader" - if a NEWER producer adds a field
 * (say {@code couponCode}), OLD consumers deserialize without crashing.
 * Rule of thumb for evolving events: only ADD optional fields; never rename,
 * remove, or change the meaning of existing ones.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        UUID orderId,
        String userId,
        String productId,
        int quantity,
        BigDecimal totalAmount,
        Instant occurredAt
) {
}
