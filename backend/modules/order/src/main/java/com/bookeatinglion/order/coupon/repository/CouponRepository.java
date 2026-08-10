package com.bookeatinglion.order.coupon.repository;

import com.bookeatinglion.order.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {}
