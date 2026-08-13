package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * The order could not be fulfilled. Published after any compensating actions
 * have been sent.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledEvent(
        UUID orderId,
        String userId,
        String reason,
        Instant occurredAt
) {
}
