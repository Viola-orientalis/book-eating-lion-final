package com.bookeatinglion.order.payment.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.common.security.SecurityUtils;
import com.bookeatinglion.order.payment.dto.PaymentApproveRequest;
import com.bookeatinglion.order.payment.dto.PaymentCancelRequest;
import com.bookeatinglion.order.payment.dto.PaymentResponse;
import com.bookeatinglion.order.payment.dto.ReceiptResponse;
import com.bookeatinglion.order.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentResponse> approve(@Valid @RequestBody PaymentApproveRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(paymentService.approve(memberId, request));
    }

    @GetMapping("/{paymentId}/receipt")
    public ApiResponse<ReceiptResponse> getReceipt(@PathVariable Long paymentId) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(paymentService.getReceipt(memberId, paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long paymentId, @Valid @RequestBody PaymentCancelRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        paymentService.cancel(memberId, paymentId, request);
        return ApiResponse.success(null);
    }
}
