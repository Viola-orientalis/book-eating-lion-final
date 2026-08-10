package com.bookeatinglion.order.dto;

import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long memberId,
        String recipientName,
        String recipientPhone,
        String postalCode,
        String address,
        String addressDetail,
        long itemsSubtotal,
        long couponDiscountAmount,
        long usedPoint,
        long totalAmount,
        OrderStatus orderStatus,
        List<OrderItemResponse> items,
        LocalDateTime createdAt) {

    public static OrderResponse of(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getMemberId(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getPostalCode(),
                order.getAddress(),
                order.getAddressDetail(),
                order.getItemsSubtotal(),
                order.getCouponDiscountAmount(),
                order.getUsedPoint(),
                order.getTotalAmount(),
                order.getOrderStatus(),
                items,
                order.getCreatedAt());
    }
}
