package com.shopflow.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * THE EVENT STORE (event sourcing).
 *
 * Instead of only keeping the latest state ("order X is CANCELLED") we keep an
 * APPEND-ONLY sequence of immutable facts:
 *     seq 1: OrderCreated {qty: 2, total: 239.00}
 *     seq 2: InventoryReserved
 *     seq 3: PaymentFailed {reason: "insufficient funds"}
 *     seq 4: OrderCancelled
 * Current state = fold(events). Nothing is ever UPDATEd or DELETEd here.
 *
 * WHY BOTHER?
 *  - Complete audit trail for free ("WHY is this order cancelled?" is answerable)
 *  - Temporal queries (state as of any point in time)
 *  - New read models can be built later by replaying history
 *  - Natural fit with Kafka: each appended event is also published to the
 *    ORDER_EVENTS topic, so the log is both stored AND streamed.
 *
 * The (orderId, seqNo) unique constraint gives per-aggregate ordering and
 * doubles as an optimistic concurrency check on append.
 */
@Entity
@Table(name = "order_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"orderId", "seqNo"}),
        indexes = @Index(columnList = "orderId"))
public class OrderEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID orderId;

    /** Position within THIS order's stream: 1, 2, 3... */
    @Column(nullable = false)
    private int seqNo;

    /** e.g. "OrderCreatedEvent" - used to pick the right class on replay. */
    @Column(nullable = false)
    private String eventType;

    /** The event serialized as JSON - schema-flexible, human-readable. */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant occurredAt;

    protected OrderEventEntity() {
    }

    public OrderEventEntity(UUID orderId, int seqNo, String eventType, String payload) {
        this.orderId = orderId;
        this.seqNo = seqNo;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public int getSeqNo() { return seqNo; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
}
