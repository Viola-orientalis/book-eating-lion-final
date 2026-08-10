package com.bookeatinglion.order.coupon.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.common.security.SecurityUtils;
import com.bookeatinglion.order.coupon.dto.MemberCouponResponse;
import com.bookeatinglion.order.coupon.service.CouponService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/me")
    public ApiResponse<List<MemberCouponResponse>> listMyCoupons() {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(couponService.listMyCoupons(memberId));
    }
}
