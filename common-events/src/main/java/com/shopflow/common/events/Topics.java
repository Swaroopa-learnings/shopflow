package com.shopflow.common.events;

/**
 * Central registry of Kafka topic names so producers and consumers can never
 * drift apart because of a typo in a string literal.
 *
 * TOPIC DESIGN (worth explaining in an interview):
 *  - ORDER_EVENTS is an *event log*: facts about what happened to orders
 *    ("OrderCreated", "OrderCompleted"). Multiple independent consumers read it
 *    (the CQRS projection, notification-service) - classic pub/sub fan-out.
 *  - The *.COMMANDS topics carry imperatives addressed to exactly ONE service
 *    ("reserve this stock", "charge this card") - point-to-point messaging.
 *  - The *.EVENTS reply topics carry the outcome back to the saga orchestrator.
 *
 * Commands say "do this" (may be rejected); events say "this happened" (immutable fact).
 */
public final class Topics {

    /** Event-sourcing log + notification fan-out. Published by order-service. */
    public static final String ORDER_EVENTS = "order.events";

    /** Commands consumed by inventory-service (reserve / release stock). */
    public static final String INVENTORY_COMMANDS = "inventory.commands";
    /** Outcomes published by inventory-service back to the saga orchestrator. */
    public static final String INVENTORY_EVENTS = "inventory.events";

    /** Commands consumed by payment-service (process payment). */
    public static final String PAYMENT_COMMANDS = "payment.commands";
    /** Outcomes published by payment-service back to the saga orchestrator. */
    public static final String PAYMENT_EVENTS = "payment.events";

    private Topics() {
    }
}
