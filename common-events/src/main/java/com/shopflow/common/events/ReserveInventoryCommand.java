package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Command (saga step 1): "inventory-service, please reserve {quantity} units of
 * {productId} for order {orderId}". May legitimately fail -> InventoryRejectedEvent.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReserveInventoryCommand(
        UUID orderId,
        String productId,
        int quantity
) {
}
