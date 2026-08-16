package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Stock could not be reserved; the saga cancels the order. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryRejectedEvent(
        UUID orderId,
        String reason
) implements InventoryReply {
}
