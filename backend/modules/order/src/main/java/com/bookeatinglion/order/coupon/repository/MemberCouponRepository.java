package com.bookeatinglion.order.coupon.repository;

import com.bookeatinglion.order.coupon.domain.MemberCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    List<MemberCoupon> findByMemberIdAndIsUsedFalse(Long memberId);
}
