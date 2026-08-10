package com.bookeatinglion.order.payment.exception;

public class UnauthorizedPaymentAccessException extends PaymentDomainException {

    public UnauthorizedPaymentAccessException(Long paymentId) {
        super(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS, "본인의 결제만 접근할 수 있습니다: paymentId=" + paymentId);
    }
}
