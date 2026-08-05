package com.shopflow.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * READ MODEL (CQRS query side) - a flat, denormalized row per order, shaped
 * exactly like the JSON the query API returns.
 *
 * It is populated ONLY by OrderProjection consuming the Kafka event log -
 * never written by command handlers. That one-way flow is what makes it CQRS:
 *
 *   commands -> write model + events        events -> read model -> queries
 *
 * Consequence to own in interviews: the read model is EVENTUALLY CONSISTENT -
 * for a few milliseconds after creation, GET may not see the order yet.
 * In exchange, reads are trivial indexed lookups that scale independently
 * (this table could live in Elasticsearch or a replica DB with zero impact
 * on the write side).
 */
@Entity
@Table(name = "order_summaries", indexes = @Index(columnList = "userId"))
public class OrderSummaryEntity {

    @Id
    private UUID orderId;

    @Column(nullable = false)
    private String userId;

    private String productId;
    private int quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** Human-readable saga outcome, e.g. "payment declined". */
    private String statusReason;

    private Instant createdAt;
    private Instant lastUpdatedAt;

    protected OrderSummaryEntity() {
    }

    public OrderSummaryEntity(UUID orderId, String userId, String productId, int quantity,
                              BigDecimal totalAmount, OrderStatus status, Instant createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.lastUpdatedAt = createdAt;
    }

    public void updateStatus(OrderStatus status, String reason) {
        this.status = status;
        this.statusReason = reason;
        this.lastUpdatedAt = Instant.now();
    }

    public UUID getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
}
