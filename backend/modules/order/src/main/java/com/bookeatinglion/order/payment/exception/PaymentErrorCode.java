package com.bookeatinglion.order.payment.exception;

import org.springframework.http.HttpStatus;

public enum PaymentErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    UNAUTHORIZED_PAYMENT_ACCESS(HttpStatus.FORBIDDEN),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    PaymentErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
