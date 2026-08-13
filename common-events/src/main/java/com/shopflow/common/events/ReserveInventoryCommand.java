package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Asks inventory-service to reserve stock for an order. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReserveInventoryCommand(
        UUID orderId,
        String productId,
        int quantity
) {
}
