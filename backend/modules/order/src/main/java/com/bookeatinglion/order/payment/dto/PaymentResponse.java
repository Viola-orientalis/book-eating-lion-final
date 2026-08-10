package com.bookeatinglion.order.payment.dto;

import com.bookeatinglion.order.payment.domain.Payment;
import com.bookeatinglion.order.payment.domain.PaymentMethod;
import com.bookeatinglion.order.payment.domain.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        PaymentMethod paymentMethod,
        String approvalNumber,
        String pgTid,
        long amount,
        PaymentStatus paymentStatus,
        LocalDateTime approvedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getApprovalNumber(),
                payment.getPgTid(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getApprovedAt());
    }
}
