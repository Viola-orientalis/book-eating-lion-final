package com.bookeatinglion.order.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bookeatinglion.order.coupon.domain.Coupon;
import com.bookeatinglion.order.coupon.domain.MemberCoupon;
import com.bookeatinglion.order.coupon.exception.CouponAlreadyUsedException;
import com.bookeatinglion.order.coupon.exception.CouponExpiredException;
import com.bookeatinglion.order.coupon.exception.CouponMinimumAmountNotMetException;
import com.bookeatinglion.order.coupon.exception.UnauthorizedCouponAccessException;
import com.bookeatinglion.order.coupon.repository.CouponRepository;
import com.bookeatinglion.order.coupon.repository.MemberCouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon coupon(long discount, long minimumOrderAmount, LocalDateTime expiresAt) {
        Coupon coupon = BeanUtils.instantiateClass(Coupon.class);
        ReflectionTestUtils.setField(coupon, "id", 1L);
        ReflectionTestUtils.setField(coupon, "discountAmount", discount);
        ReflectionTestUtils.setField(coupon, "minimumOrderAmount", minimumOrderAmount);
        ReflectionTestUtils.setField(coupon, "expiresAt", expiresAt);
        return coupon;
    }

    private MemberCoupon memberCoupon(Long memberId, Long id) {
        MemberCoupon memberCoupon = new MemberCoupon(memberId, 1L);
        ReflectionTestUtils.setField(memberCoupon, "id", id);
        return memberCoupon;
    }

    @Test
    void 유효한_쿠폰이면_사용처리하고_할인액을_반환한다() {
        MemberCoupon memberCoupon = memberCoupon(1L, 10L);
        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(1L))
                .thenReturn(
                        Optional.of(coupon(2_000, 10_000, LocalDateTime.now().plusDays(1))));

        long discount = couponService.validateAndUse(1L, 10L, 20_000);

        assertThat(discount).isEqualTo(2_000);
        assertThat(memberCoupon.isUsed()).isTrue();
    }

    @Test
    void 본인_쿠폰이_아니면_예외를_던진다() {
        MemberCoupon memberCoupon = memberCoupon(2L, 10L);
        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));

        assertThatThrownBy(() -> couponService.validateAndUse(1L, 10L, 20_000))
                .isInstanceOf(UnauthorizedCouponAccessException.class);
    }

    @Test
    void 이미_사용된_쿠폰이면_예외를_던진다() {
        MemberCoupon memberCoupon = memberCoupon(1L, 10L);
        memberCoupon.use();
        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));

        assertThatThrownBy(() -> couponService.validateAndUse(1L, 10L, 20_000))
                .isInstanceOf(CouponAlreadyUsedException.class);
    }

    @Test
    void 만료된_쿠폰이면_예외를_던진다() {
        MemberCoupon memberCoupon = memberCoupon(1L, 10L);
        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(1L))
                .thenReturn(Optional.of(coupon(2_000, 0, LocalDateTime.now().minusDays(1))));

        assertThatThrownBy(() -> couponService.validateAndUse(1L, 10L, 20_000))
                .isInstanceOf(CouponExpiredException.class);
    }

    @Test
    void 최소주문금액_미만이면_예외를_던진다() {
        MemberCoupon memberCoupon = memberCoupon(1L, 10L);
        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));
        when(couponRepository.findById(1L))
                .thenReturn(
                        Optional.of(coupon(2_000, 30_000, LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> couponService.validateAndUse(1L, 10L, 20_000))
                .isInstanceOf(CouponMinimumAmountNotMetException.class);
    }

    @Test
    void 사용된_쿠폰을_복원하면_다시_사용가능해진다() {
        MemberCoupon memberCoupon = memberCoupon(1L, 10L);
        memberCoupon.use();
        when(memberCouponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon));

        couponService.restore(10L);

        assertThat(memberCoupon.isUsed()).isFalse();
    }
}
