package com.bookeatinglion.order.coupon.dto;

import com.bookeatinglion.order.coupon.domain.Coupon;
import com.bookeatinglion.order.coupon.domain.MemberCoupon;
import java.time.LocalDateTime;

public record MemberCouponResponse(
        Long memberCouponId,
        String couponCode,
        String couponName,
        long discountAmount,
        long minimumOrderAmount,
        LocalDateTime expiresAt) {

    public static MemberCouponResponse of(MemberCoupon memberCoupon, Coupon coupon) {
        return new MemberCouponResponse(
                memberCoupon.getId(),
                coupon.getCouponCode(),
                coupon.getCouponName(),
                coupon.getDiscountAmount(),
                coupon.getMinimumOrderAmount(),
                coupon.getExpiresAt());
    }
}
