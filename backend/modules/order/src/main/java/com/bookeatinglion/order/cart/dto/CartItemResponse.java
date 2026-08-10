package com.bookeatinglion.order.cart.dto;

import com.bookeatinglion.order.cart.domain.CartItem;

public record CartItemResponse(Long cartItemId, Long bookId, int quantity) {

    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(cartItem.getId(), cartItem.getBookId(), cartItem.getQuantity());
    }
}
