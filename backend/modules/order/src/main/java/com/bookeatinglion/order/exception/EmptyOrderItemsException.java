package com.bookeatinglion.order.exception;

public class EmptyOrderItemsException extends OrderDomainException {

    public EmptyOrderItemsException() {
        super(OrderErrorCode.EMPTY_ORDER_ITEMS, "주문할 상품이 없습니다.");
    }
}
