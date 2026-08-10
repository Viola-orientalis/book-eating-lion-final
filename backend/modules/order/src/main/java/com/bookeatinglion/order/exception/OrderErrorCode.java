package com.bookeatinglion.order.exception;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ORDER_ACCESS(HttpStatus.FORBIDDEN),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    LOCK_ACQUISITION_FAILED(HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    OrderErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
