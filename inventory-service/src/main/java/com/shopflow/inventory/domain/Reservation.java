package com.shopflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One reservation per order. The order id is the primary key, so a redelivered
 * command can be recognised as a duplicate instead of reserving stock twice.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    private UUID orderId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean released;

    @Column(nullable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    public Reservation(UUID orderId, String productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.released = false;
        this.createdAt = Instant.now();
    }

    public void markReleased() {
        this.released = true;
    }

    public UUID getOrderId() { return orderId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public boolean isReleased() { return released; }
    public Instant getCreatedAt() { return createdAt; }
}
