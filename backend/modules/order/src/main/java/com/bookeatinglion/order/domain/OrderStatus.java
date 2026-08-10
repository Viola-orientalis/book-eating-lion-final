package com.bookeatinglion.order.domain;

/** db/cluster-a/03-order_db.sql 의 chk_orders_status 값과 1:1. */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCEL_REQUESTED,
    CANCELLED,
    EXCHANGE_REQUESTED,
    EXCHANGED,
    RETURN_REQUESTED,
    RETURNED,
    REFUND_REQUESTED,
    REFUNDED
}
