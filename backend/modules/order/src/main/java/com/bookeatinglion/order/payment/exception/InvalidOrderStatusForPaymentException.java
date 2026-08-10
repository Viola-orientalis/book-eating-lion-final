package com.bookeatinglion.order.payment.exception;

import com.bookeatinglion.order.domain.OrderStatus;

public class InvalidOrderStatusForPaymentException extends PaymentDomainException {

    public InvalidOrderStatusForPaymentException(Long orderId, OrderStatus actual) {
        super(PaymentErrorCode.INVALID_ORDER_STATUS, "결제 가능한 주문 상태가 아닙니다: orderId=" + orderId + ", status=" + actual);
    }
}
