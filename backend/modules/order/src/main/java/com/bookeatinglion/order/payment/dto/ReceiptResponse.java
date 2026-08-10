package com.bookeatinglion.order.payment.dto;

import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.dto.OrderItemResponse;
import com.bookeatinglion.order.payment.domain.Payment;
import com.bookeatinglion.order.payment.domain.PaymentMethod;
import com.bookeatinglion.order.payment.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ReceiptResponse(
        Long paymentId,
        Long orderId,
        String approvalNumber,
        String pgTid,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        long itemsSubtotal,
        long couponDiscountAmount,
        long usedPoint,
        long amount,
        List<OrderItemResponse> items,
        LocalDateTime approvedAt) {

    public static ReceiptResponse of(Payment payment, Order order, List<OrderItemResponse> items) {
        return new ReceiptResponse(
                payment.getId(),
                order.getId(),
                payment.getApprovalNumber(),
                payment.getPgTid(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                order.getItemsSubtotal(),
                order.getCouponDiscountAmount(),
                order.getUsedPoint(),
                payment.getAmount(),
                items,
                payment.getApprovedAt());
    }
}
