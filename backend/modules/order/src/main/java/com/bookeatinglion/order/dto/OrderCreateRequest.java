package com.bookeatinglion.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        Long addressId,
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        @NotBlank String postalCode,
        @NotBlank String address,
        String addressDetail,
        Long memberCouponId,
        @Min(0) Long usedPoint,
        /** 있으면 주문 성공 후 이 장바구니 항목들을 삭제한다(체크아웃 정리용). */
        List<Long> cartItemIds) {

    public long usedPointOrZero() {
        return usedPoint == null ? 0L : usedPoint;
    }
}
