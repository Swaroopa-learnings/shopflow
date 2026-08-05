package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Fact: the order could not be fulfilled (no stock, payment declined, or timed
 * out). Emitted after the saga has run its compensating actions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledEvent(
        UUID orderId,
        String userId,
        String reason,
        Instant occurredAt
) {
}
