package com.shopflow.common.events;

import java.util.UUID;

/**
 * The closed set of replies inventory-service sends back to the saga.
 * Sealed so only a real outcome can be published to inventory.events.
 */
public sealed interface InventoryReply
        permits InventoryReservedEvent, InventoryRejectedEvent {

    UUID orderId();
}
