package com.bookeatinglion.order.dto;

import com.bookeatinglion.order.domain.OrderItem;

public record OrderItemResponse(Long orderItemId, Long bookId, String bookTitle, int quantity, long unitPrice) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(), item.getBookId(), item.getBookTitle(), item.getQuantity(), item.getUnitPrice());
    }
}
