package com.bookeatinglion.order.coupon.exception;

public class MemberCouponNotFoundException extends CouponDomainException {

    public MemberCouponNotFoundException(Long memberCouponId) {
        super(CouponErrorCode.MEMBER_COUPON_NOT_FOUND, "보유 쿠폰을 찾을 수 없습니다: memberCouponId=" + memberCouponId);
    }
}
