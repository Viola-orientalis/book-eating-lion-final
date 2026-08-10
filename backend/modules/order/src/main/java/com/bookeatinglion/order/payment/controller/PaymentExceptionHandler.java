package com.bookeatinglion.order.payment.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.order.exception.OrderDomainException;
import com.bookeatinglion.order.payment.exception.PaymentDomainException;
import com.bookeatinglion.order.payment.exception.PaymentErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.bookeatinglion.order.payment.controller")
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentDomainException(PaymentDomainException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(OrderDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderDomainException(OrderDomainException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
    }

    /**
     * uk_payments_idempotency 경쟁(동시에 같은 키로 두 요청) 또는 uk_payments_approval_number
     * 충돌. 이미 flush 된 영속성 컨텍스트를 이 트랜잭션에서 계속 쓰지 않고, 클라이언트가 같은
     * idempotencyKey 로 재요청하도록 안내한다 — 재요청은 서비스의 조회 분기에서 안전하게 처리된다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateRequest(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "DUPLICATE_PAYMENT_REQUEST", "이미 처리 중이거나 처리된 결제 요청입니다. 같은 idempotencyKey 로 다시 조회하세요."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("INVALID_STATE", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(PaymentErrorCode.INVALID_REQUEST.name(), message));
    }
}
