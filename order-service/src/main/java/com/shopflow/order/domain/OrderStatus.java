package com.shopflow.order.domain;

/**
 * Order lifecycle = the saga's state machine.
 *
 *  PENDING            (created; ReserveInventoryCommand sent)
 *    -> AWAITING_PAYMENT  (stock reserved; ProcessPaymentCommand sent)
 *         -> COMPLETED    (payment ok - happy path ends)
 *         -> CANCELLED    (payment failed - stock released via compensation)
 *    -> CANCELLED         (no stock, or stuck PENDING past the timeout sweep)
 */
public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    COMPLETED,
    CANCELLED
}
