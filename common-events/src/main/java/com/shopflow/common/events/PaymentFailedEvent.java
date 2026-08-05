package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Saga reply: payment declined / bank unreachable. The orchestrator must now
 * COMPENSATE the earlier step (release the reserved stock) and cancel the order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedEvent(
        UUID orderId,
        String reason
) {
}
