package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Saga reply: not enough stock - orchestrator cancels the order (no compensation needed, nothing was done yet). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryRejectedEvent(
        UUID orderId,
        String reason
) {
}
