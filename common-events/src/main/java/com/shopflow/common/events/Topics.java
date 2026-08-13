package com.shopflow.common.events;

/**
 * Kafka topic names, kept in one place so producers and consumers agree.
 *
 * Commands ask one service to do something and can be refused; events record
 * something that already happened and may have many readers.
 */
public final class Topics {

    /** Order events, published by order-service and read by several services. */
    public static final String ORDER_EVENTS = "order.events";

    /** Stock commands for inventory-service. */
    public static final String INVENTORY_COMMANDS = "inventory.commands";
    /** Inventory outcomes, read by the saga. */
    public static final String INVENTORY_EVENTS = "inventory.events";

    /** Payment commands for payment-service. */
    public static final String PAYMENT_COMMANDS = "payment.commands";
    /** Payment outcomes, read by the saga. */
    public static final String PAYMENT_EVENTS = "payment.events";

    private Topics() {
    }
}
