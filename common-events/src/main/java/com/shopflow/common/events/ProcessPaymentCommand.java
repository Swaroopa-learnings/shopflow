package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** Command (saga step 2): "payment-service, please charge {amount} for order {orderId}". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessPaymentCommand(
        UUID orderId,
        String userId,
        BigDecimal amount
) {
}
