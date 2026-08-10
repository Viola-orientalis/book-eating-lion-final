package com.bookeatinglion.order.delivery.domain;

/** db/cluster-a/03-order_db.sql 의 chk_deliveries_status 값과 1:1. */
public enum DeliveryStatus {
    READY,
    SHIPPED,
    IN_TRANSIT,
    DELIVERED
}
