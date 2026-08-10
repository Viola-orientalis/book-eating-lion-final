package com.bookeatinglion.order.coupon.exception;

public class CouponMinimumAmountNotMetException extends CouponDomainException {

    public CouponMinimumAmountNotMetException(Long memberCouponId, long minimumOrderAmount) {
        super(
                CouponErrorCode.COUPON_MINIMUM_AMOUNT_NOT_MET,
                "최소 주문 금액(" + minimumOrderAmount + "원) 미만이라 쿠폰을 적용할 수 없습니다: memberCouponId=" + memberCouponId);
    }
}
