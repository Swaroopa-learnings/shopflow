package com.shopflow.common.events;

import java.util.UUID;

/**
 * The closed set of events that belong in an order's history.
 *
 * Sealed on purpose: the event store is the source of truth for an order, so
 * only these types may be appended to it. A command or an unrelated object
 * will not compile.
 */
public sealed interface OrderEvent
        permits OrderCreatedEvent, OrderCompletedEvent, OrderCancelledEvent {

    /** The order this event belongs to. */
    UUID orderId();
}
