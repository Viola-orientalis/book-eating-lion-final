package com.bookeatinglion.order.coupon.exception;

public class CouponExpiredException extends CouponDomainException {

    public CouponExpiredException(Long memberCouponId) {
        super(CouponErrorCode.COUPON_EXPIRED, "만료된 쿠폰입니다: memberCouponId=" + memberCouponId);
    }
}
