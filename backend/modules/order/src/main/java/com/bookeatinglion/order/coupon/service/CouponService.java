package com.bookeatinglion.order.coupon.service;

import com.bookeatinglion.order.coupon.domain.Coupon;
import com.bookeatinglion.order.coupon.domain.MemberCoupon;
import com.bookeatinglion.order.coupon.dto.MemberCouponResponse;
import com.bookeatinglion.order.coupon.exception.CouponAlreadyUsedException;
import com.bookeatinglion.order.coupon.exception.CouponExpiredException;
import com.bookeatinglion.order.coupon.exception.CouponMinimumAmountNotMetException;
import com.bookeatinglion.order.coupon.exception.MemberCouponNotFoundException;
import com.bookeatinglion.order.coupon.exception.UnauthorizedCouponAccessException;
import com.bookeatinglion.order.coupon.repository.CouponRepository;
import com.bookeatinglion.order.coupon.repository.MemberCouponRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;

    public List<MemberCouponResponse> listMyCoupons(Long memberId) {
        return memberCouponRepository.findByMemberIdAndIsUsedFalse(memberId).stream()
                .map(mc -> MemberCouponResponse.of(mc, requireCoupon(mc.getCouponId())))
                .toList();
    }

    /**
     * 주문 생성 트랜잭션 안에서 호출된다(OrderService). 소유자·미사용·만료·최소주문액을 검증하고
     * 쿠폰을 "사용" 처리한 뒤 할인액을 반환한다 — 재고 차감과 같은 로컬 트랜잭션에 묶인다.
     */
    @Transactional
    public long validateAndUse(Long memberId, Long memberCouponId, long itemsSubtotal) {
        MemberCoupon memberCoupon = memberCouponRepository
                .findById(memberCouponId)
                .orElseThrow(() -> new MemberCouponNotFoundException(memberCouponId));

        if (!memberCoupon.isOwnedBy(memberId)) {
            throw new UnauthorizedCouponAccessException(memberCouponId);
        }
        if (memberCoupon.isUsed()) {
            throw new CouponAlreadyUsedException(memberCouponId);
        }

        Coupon coupon = requireCoupon(memberCoupon.getCouponId());
        if (coupon.isExpired()) {
            throw new CouponExpiredException(memberCouponId);
        }
        if (!coupon.isApplicableTo(itemsSubtotal)) {
            throw new CouponMinimumAmountNotMetException(memberCouponId, coupon.getMinimumOrderAmount());
        }

        memberCoupon.use();
        return coupon.getDiscountAmount();
    }

    /** 결제 취소·환불 시 쿠폰 사용 상태를 원복한다. */
    @Transactional
    public void restore(Long memberCouponId) {
        MemberCoupon memberCoupon = memberCouponRepository
                .findById(memberCouponId)
                .orElseThrow(() -> new MemberCouponNotFoundException(memberCouponId));
        memberCoupon.restore();
    }

    private Coupon requireCoupon(Long couponId) {
        return couponRepository.findById(couponId).orElseThrow(() -> new MemberCouponNotFoundException(couponId));
    }
}
