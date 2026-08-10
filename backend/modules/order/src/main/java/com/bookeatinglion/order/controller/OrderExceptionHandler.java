package com.bookeatinglion.order.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.order.coupon.exception.CouponDomainException;
import com.bookeatinglion.order.exception.OrderDomainException;
import com.bookeatinglion.order.exception.OrderErrorCode;
import com.bookeatinglion.order.inventory.domain.InsufficientStockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.bookeatinglion.order.controller")
public class OrderExceptionHandler {

    @ExceptionHandler(OrderDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderDomainException(OrderDomainException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(CouponDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponDomainException(CouponDomainException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(OrderErrorCode.INSUFFICIENT_STOCK.name(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(OrderErrorCode.INVALID_REQUEST.name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(OrderErrorCode.INVALID_REQUEST.name(), message));
    }
}
