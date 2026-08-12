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
 * ASYNC entry point - this service's "controller" is a Kafka listener, not an
 * HTTP endpoint.
 *
 * MULTI-TYPE TOPIC DISPATCH: one topic (inventory.commands) carries several
 * command types, so we use the idiomatic Spring Kafka pattern:
 *
 *   @KafkaListener on the CLASS  -> one consumer, one container for the topic
 *   @KafkaHandler  on METHODS    -> Spring routes each message to the method
 *                                   whose parameter type matches the payload
 *
 * The producer's JsonSerializer writes a __TypeId__ header naming the concrete
 * class; the consumer's JsonDeserializer uses it to rebuild that exact type,
 * and @KafkaHandler then picks the matching method. No instanceof chain, and
 * adding a new command type is just adding a new method.
 *
 * WHY NOT A SINGLE METHOD TAKING Object? (a trap worth knowing)
 * Spring Kafka supplies "provided arguments" - the raw ConsumerRecord, the
 * Acknowledgment, the Consumer - and matches them to parameters BY
 * ASSIGNABILITY, before any payload resolution happens. Everything is
 * assignable to Object, so such a parameter silently receives the raw
 * ConsumerRecord instead of the deserialized command. Every instanceof then
 * fails, the message is consumed, the offset advances, and nothing happens -
 * with no error logged anywhere. (@Payload does not help; provided-argument
 * matching wins first.) Concrete parameter types avoid the ambiguity entirely.
 *
 * groupId "inventory-service": if this service scales to 3 instances, the 3
 * consumers share ONE group => Kafka assigns each partition to exactly one of
 * them => each command is processed once (per group), with per-key ordering
 * preserved (all commands for one order share a partition via the orderId key).
 */
@Component
@KafkaListener(topics = Topics.INVENTORY_COMMANDS, groupId = "inventory-service")
public class InventoryCommandListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandListener.class);

    private final InventoryService inventoryService;

    public InventoryCommandListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** Saga step 1: reserve stock for an order. */
    @KafkaHandler
    public void onReserve(ReserveInventoryCommand command) {
        inventoryService.reserve(command);
    }

    /** Compensation: give the stock back when a later saga step failed. */
    @KafkaHandler
    public void onRelease(ReleaseInventoryCommand command) {
        inventoryService.release(command);
    }

    /**
     * Fallback for any payload that matches no handler above. Without this,
     * Spring throws (noisy but at least visible); with it we log the actual
     * type, which is what you need when a producer starts sending something
     * new or a type header goes missing. Never let a message vanish silently.
     */
    @KafkaHandler(isDefault = true)
    public void onUnknown(Object payload) {
        log.error("Unhandled command payload of type {} - saga will stall. Payload: {}",
                payload == null ? "null" : payload.getClass().getName(), payload);
    }
}
