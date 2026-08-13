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
 * One stored event in an order's history. Append-only: rows are never updated
 * or deleted, so the table doubles as an audit trail and can be replayed to
 * rebuild state.
 *
 * The (orderId, seqNo) unique constraint keeps each order's events ordered.
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

    /** Position in this order's stream: 1, 2, 3... */
    @Column(nullable = false)
    private int seqNo;

    /** Class name, used to pick the right type when replaying. */
    @Column(nullable = false)
    private String eventType;

    /** The event serialized as JSON. */
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
