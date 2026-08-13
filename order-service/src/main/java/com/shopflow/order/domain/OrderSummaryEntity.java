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
 * Read model: one flat row per order, shaped like the JSON the query API
 * returns. Written only by OrderProjection from the event stream, never by
 * command handlers.
 *
 * Because it is updated asynchronously it is eventually consistent - a query
 * made immediately after creation may not see the order yet.
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

    /** Why the order ended in this state, e.g. "payment declined". */
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
