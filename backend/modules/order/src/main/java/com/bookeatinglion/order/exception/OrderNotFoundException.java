package com.bookeatinglion.order.exception;

public class OrderNotFoundException extends OrderDomainException {

    public OrderNotFoundException(Long orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다: orderId=" + orderId);
    }
}
