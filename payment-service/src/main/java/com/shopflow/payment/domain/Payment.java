package com.shopflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment record - PK is the ORDER id: same idempotent-consumer trick as
 * inventory's Reservation. A redelivered ProcessPaymentCommand finds this row
 * and does NOT charge the customer twice.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;        // CAPTURED / FAILED

    private String bankReference; // set on success

    private String failureReason; // set on failure

    @Column(nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public static Payment captured(UUID orderId, String userId, BigDecimal amount, String bankReference) {
        Payment p = base(orderId, userId, amount);
        p.status = "CAPTURED";
        p.bankReference = bankReference;
        return p;
    }

    public static Payment failed(UUID orderId, String userId, BigDecimal amount, String reason) {
        Payment p = base(orderId, userId, amount);
        p.status = "FAILED";
        p.failureReason = reason;
        return p;
    }

    private static Payment base(UUID orderId, String userId, BigDecimal amount) {
        Payment p = new Payment();
        p.orderId = orderId;
        p.userId = userId;
        p.amount = amount;
        p.createdAt = Instant.now();
        return p;
    }

    public UUID getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getBankReference() { return bankReference; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
