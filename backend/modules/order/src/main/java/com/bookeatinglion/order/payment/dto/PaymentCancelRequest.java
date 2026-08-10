package com.bookeatinglion.order.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCancelRequest(@NotBlank String reason) {}
