package com.bookeatinglion.order.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.common.security.SecurityUtils;
import com.bookeatinglion.order.dto.OrderCreateRequest;
import com.bookeatinglion.order.dto.OrderResponse;
import com.bookeatinglion.order.dto.OrderSummaryResponse;
import com.bookeatinglion.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(orderService.createOrder(memberId, request));
    }

    @GetMapping
    public ApiResponse<Page<OrderSummaryResponse>> getMyOrders(@PageableDefault(size = 10) Pageable pageable) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(orderService.getMyOrders(memberId, pageable));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(orderService.getOrder(memberId, orderId));
    }
}
