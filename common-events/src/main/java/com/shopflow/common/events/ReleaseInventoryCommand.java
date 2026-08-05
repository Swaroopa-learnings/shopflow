package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * COMPENSATING command: undo a previous reservation because a later saga step
 * (payment) failed. Compensation is the heart of the saga pattern - since
 * there is no distributed ACID rollback across services, each step must define
 * its own semantic "undo".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseInventoryCommand(
        UUID orderId,
        String reason
) {
}
