package com.bookeatinglion.order.cart.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestCartMergeRequest(@NotBlank String guestId) {}
