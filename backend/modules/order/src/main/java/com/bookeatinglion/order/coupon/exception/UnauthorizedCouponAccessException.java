package com.bookeatinglion.order.coupon.exception;

public class UnauthorizedCouponAccessException extends CouponDomainException {

    public UnauthorizedCouponAccessException(Long memberCouponId) {
        super(CouponErrorCode.UNAUTHORIZED_COUPON_ACCESS, "본인 소유의 쿠폰이 아닙니다: memberCouponId=" + memberCouponId);
    }
}
