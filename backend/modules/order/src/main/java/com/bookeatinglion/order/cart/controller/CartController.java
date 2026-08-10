package com.bookeatinglion.order.cart.controller;

import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.common.security.SecurityUtils;
import com.bookeatinglion.order.cart.dto.CartItemAddRequest;
import com.bookeatinglion.order.cart.dto.CartItemResponse;
import com.bookeatinglion.order.cart.dto.CartItemUpdateRequest;
import com.bookeatinglion.order.cart.dto.GuestCartItemAddRequest;
import com.bookeatinglion.order.cart.dto.GuestCartMergeRequest;
import com.bookeatinglion.order.cart.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public ApiResponse<List<CartItemResponse>> list() {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(cartService.list(memberId));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemResponse> addItem(@Valid @RequestBody CartItemAddRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(cartService.addItem(memberId, request.bookId(), request.quantity()));
    }

    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<CartItemResponse> updateQuantity(
            @PathVariable Long cartItemId, @Valid @RequestBody CartItemUpdateRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(cartService.updateQuantity(memberId, cartItemId, request.quantity()));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long cartItemId) {
        Long memberId = SecurityUtils.currentMemberId();
        cartService.removeItem(memberId, cartItemId);
        return ApiResponse.success(null);
    }

    /**
     * 비회원 카트 담기. 로그인 전이라 인증이 없다(SecurityConfig 에서 permitAll) — memberId 대신
     * 프론트가 발급한 guestId 로 Redis 해시에 쌓는다.
     */
    @PostMapping("/guest-items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addGuestItem(@Valid @RequestBody GuestCartItemAddRequest request) {
        cartService.addGuestItem(request.guestId(), request.bookId(), request.quantity());
        return ApiResponse.success(null);
    }

    @PostMapping("/merge")
    public ApiResponse<List<CartItemResponse>> merge(@Valid @RequestBody GuestCartMergeRequest request) {
        Long memberId = SecurityUtils.currentMemberId();
        cartService.mergeGuestCart(memberId, request.guestId());
        return ApiResponse.success(cartService.list(memberId));
    }
}
