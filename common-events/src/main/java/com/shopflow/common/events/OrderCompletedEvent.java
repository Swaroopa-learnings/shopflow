package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Stock was reserved and payment captured. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCompletedEvent(
        UUID orderId,
        String userId,
        Instant occurredAt
) {
}
