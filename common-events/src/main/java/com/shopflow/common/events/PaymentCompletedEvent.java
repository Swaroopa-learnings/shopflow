package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** Saga reply: money captured - orchestrator marks the order COMPLETED. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedEvent(
        UUID orderId,
        String paymentReference,
        BigDecimal amount
) {
}
