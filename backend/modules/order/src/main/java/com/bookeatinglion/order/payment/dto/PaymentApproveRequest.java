package com.bookeatinglion.order.payment.dto;

import com.bookeatinglion.order.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentApproveRequest(
        @NotNull Long orderId, @NotNull PaymentMethod paymentMethod, Long cardId, @NotBlank String idempotencyKey) {}
