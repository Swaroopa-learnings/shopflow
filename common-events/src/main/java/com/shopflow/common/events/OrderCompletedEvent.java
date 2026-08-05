package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Fact: saga finished successfully - stock reserved AND payment captured. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCompletedEvent(
        UUID orderId,
        String userId,
        Instant occurredAt
) {
}
