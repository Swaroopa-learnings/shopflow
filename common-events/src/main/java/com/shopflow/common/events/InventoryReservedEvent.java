package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Stock was reserved; the saga moves on to payment. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReservedEvent(
        UUID orderId,
        String productId,
        int quantity
) {
}
