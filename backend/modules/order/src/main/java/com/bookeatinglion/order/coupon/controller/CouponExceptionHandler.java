package com.bookeatinglion.order.coupon.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.order.coupon.exception.CouponDomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.bookeatinglion.order.coupon.controller")
public class CouponExceptionHandler {

    @ExceptionHandler(CouponDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponDomainException(CouponDomainException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }
}
