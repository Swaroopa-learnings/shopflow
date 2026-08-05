package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Saga reply: stock reservation succeeded - orchestrator proceeds to payment. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReservedEvent(
        UUID orderId,
        String productId,
        int quantity
) {
}
