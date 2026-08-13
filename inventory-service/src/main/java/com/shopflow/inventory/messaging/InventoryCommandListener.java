package com.shopflow.inventory.messaging;

import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.ReserveInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Entry point for this service - a Kafka listener rather than an HTTP endpoint.
 *
 * The topic carries several command types, so @KafkaListener sits on the class
 * and each @KafkaHandler method handles one type. Spring picks the method from
 * the payload's type, which the producer names in a header.
 *
 * Note: a single handler taking a bare Object would receive the raw
 * ConsumerRecord instead of the payload, so handler parameters are concrete types.
 *
 * All instances share one groupId, so each command is processed once.
 */
@Component
@KafkaListener(topics = Topics.INVENTORY_COMMANDS, groupId = "inventory-service")
public class InventoryCommandListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandListener.class);

    private final InventoryService inventoryService;

    public InventoryCommandListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** Reserves stock for an order. */
    @KafkaHandler
    public void onReserve(ReserveInventoryCommand command) {
        inventoryService.reserve(command);
    }

    /** Gives stock back when a later saga step failed. */
    @KafkaHandler
    public void onRelease(ReleaseInventoryCommand command) {
        inventoryService.release(command);
    }

    /** Logs any payload that matches no handler above, instead of dropping it. */
    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.error("Unhandled command payload of type {} - saga will stall. Payload: {}",
                payload == null ? "null" : payload.getClass().getName(), payload);
    }
}
