package com.shopflow.inventory.messaging;

import com.shopflow.common.events.ReleaseInventoryCommand;
import com.shopflow.common.events.ReserveInventoryCommand;
import com.shopflow.common.events.Topics;
import com.shopflow.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ASYNC entry point - this service's "controller" is a Kafka listener, not an
 * HTTP endpoint. The JsonDeserializer reconstructs the concrete command class
 * from the __TypeId__ header the producer wrote; we dispatch on type.
 *
 * groupId "inventory-service": if this service scales to 3 instances, the 3
 * consumers share ONE group => Kafka assigns each partition to exactly one of
 * them => each command is processed once (per group), with per-key ordering
 * preserved (all commands for one order share a partition via the orderId key).
 */
@Component
public class InventoryCommandListener {

    private final InventoryService inventoryService;

    public InventoryCommandListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = Topics.INVENTORY_COMMANDS, groupId = "inventory-service")
    public void onCommand(Object command) {
        if (command instanceof ReserveInventoryCommand reserve) {
            inventoryService.reserve(reserve);
        } else if (command instanceof ReleaseInventoryCommand release) {
            inventoryService.release(release);
        }
    }
}
