package com.bookeatinglion.order.payment.exception;

public abstract class PaymentDomainException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    protected PaymentDomainException(PaymentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentErrorCode getErrorCode() {
        return errorCode;
    }
}
