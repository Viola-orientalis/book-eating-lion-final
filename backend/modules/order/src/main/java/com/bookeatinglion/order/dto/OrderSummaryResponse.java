package com.bookeatinglion.order.dto;

import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.domain.OrderStatus;
import java.time.LocalDateTime;

/** 마이페이지 주문 목록용 — 아이템 상세 없이 요약만. */
public record OrderSummaryResponse(
        Long orderId, long totalAmount, OrderStatus orderStatus, int itemCount, LocalDateTime createdAt) {

    public static OrderSummaryResponse of(Order order, int itemCount) {
        return new OrderSummaryResponse(
                order.getId(), order.getTotalAmount(), order.getOrderStatus(), itemCount, order.getCreatedAt());
    }
}
