package com.bookeatinglion.order.exception;

public class UnauthorizedOrderAccessException extends OrderDomainException {

    public UnauthorizedOrderAccessException(Long orderId) {
        super(OrderErrorCode.UNAUTHORIZED_ORDER_ACCESS, "본인의 주문만 접근할 수 있습니다: orderId=" + orderId);
    }
}
