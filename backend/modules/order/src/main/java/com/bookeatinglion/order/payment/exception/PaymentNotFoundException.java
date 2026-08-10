package com.bookeatinglion.order.payment.exception;

public class PaymentNotFoundException extends PaymentDomainException {

    public PaymentNotFoundException(Long paymentId) {
        super(PaymentErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다: paymentId=" + paymentId);
    }
}
