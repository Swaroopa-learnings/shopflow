package com.shopflow.order.domain;

/**
 * Order lifecycle:
 *
 *  PENDING -> AWAITING_PAYMENT -> COMPLETED
 *     |              |
 *     +--------------+-----------> CANCELLED
 */
public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    COMPLETED,
    CANCELLED
}
