package com.shopflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Stock level per product. `available` vs `reserved` split: reserving moves
 * units between the two columns; completing a sale would decrement `reserved`;
 * compensation moves them back. Money-like bookkeeping - never lose units.
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

    /** Optimistic lock: two concurrent reservations for the last unit -> one
     *  wins, the other gets OptimisticLockException and (on retry) a rejection.
     *  No overselling without pessimistic DB locks. */
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
