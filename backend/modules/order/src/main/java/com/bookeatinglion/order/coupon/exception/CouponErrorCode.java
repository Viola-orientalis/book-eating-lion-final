package com.bookeatinglion.order.coupon.exception;

import org.springframework.http.HttpStatus;

public enum CouponErrorCode {
    MEMBER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND),
    UNAUTHORIZED_COUPON_ACCESS(HttpStatus.FORBIDDEN),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST),
    COUPON_MINIMUM_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    CouponErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
