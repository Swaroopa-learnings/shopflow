package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** Asks payment-service to charge for an order. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessPaymentCommand(
        UUID orderId,
        String userId,
        BigDecimal amount
) {
}
