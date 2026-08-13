package com.shopflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Stock level for one product. Reserving moves units from available to
 * reserved; releasing moves them back.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    private String productId;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    /** Optimistic lock: stops two concurrent reservations overselling the last unit. */
    @Version
    private Long version;

    protected InventoryItem() {
    }

    public InventoryItem(String productId, int available) {
        this.productId = productId;
        this.available = available;
        this.reserved = 0;
    }

    public boolean tryReserve(int quantity) {
        if (available < quantity) {
            return false;
        }
        available -= quantity;
        reserved += quantity;
        return true;
    }

    public void release(int quantity) {
        int toRelease = Math.min(quantity, reserved);
        reserved -= toRelease;
        available += toRelease;
    }

    public String getProductId() { return productId; }
    public int getAvailable() { return available; }
    public int getReserved() { return reserved; }
}
