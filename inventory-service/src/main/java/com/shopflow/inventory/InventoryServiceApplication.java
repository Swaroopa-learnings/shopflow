package com.shopflow.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * INVENTORY-SERVICE - saga PARTICIPANT (contrast with the ORCHESTRATOR in
 * order-service). It has no idea a "saga" exists: it just consumes commands
 * from its topic, runs a LOCAL ACID transaction on its own database, and
 * publishes the outcome. That ignorance is a feature - participants stay
 * simple and independently testable.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
