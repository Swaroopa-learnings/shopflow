package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Undoes a reservation after a later step failed. Compensation replaces the
 * rollback that a single transaction would have given.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseInventoryCommand(
        UUID orderId,
        String reason
) {
}
