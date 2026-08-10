package com.bookeatinglion.order.cart.service;

import com.bookeatinglion.order.cart.domain.CartItem;
import com.bookeatinglion.order.cart.dto.CartItemResponse;
import com.bookeatinglion.order.cart.exception.CartItemNotFoundException;
import com.bookeatinglion.order.cart.exception.UnauthorizedCartAccessException;
import com.bookeatinglion.order.cart.repository.CartItemRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장바구니 CRUD + 비회원 Redis 장바구니 병합.
 *
 * 비회원 카트는 프론트가 {@code guestId}(디바이스/세션 식별자)를 발급해 Redis 해시
 * {@code cart:guest:{guestId}} 에 bookId→quantity 로 직접 쌓는다. 로그인에 성공하면
 * {@link #mergeGuestCart} 가 그 해시를 읽어 DB 장바구니에 증분 병합하고 키를 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private static final String GUEST_CART_KEY_PREFIX = "cart:guest:";
    private static final Duration GUEST_CART_TTL = Duration.ofDays(30);

    private final CartItemRepository cartItemRepository;
    private final StringRedisTemplate redisTemplate;

    public List<CartItemResponse> list(Long memberId) {
        return cartItemRepository.findByMemberId(memberId).stream()
                .map(CartItemResponse::from)
                .toList();
    }

    @Transactional
    public CartItemResponse addItem(Long memberId, Long bookId, int quantity) {
        CartItem cartItem = cartItemRepository
                .findByMemberIdAndBookId(memberId, bookId)
                .map(existing -> {
                    existing.increaseQuantity(quantity);
                    return existing;
                })
                .orElseGet(() -> cartItemRepository.save(new CartItem(memberId, bookId, quantity)));
        return CartItemResponse.from(cartItem);
    }

    @Transactional
    public CartItemResponse updateQuantity(Long memberId, Long cartItemId, int quantity) {
        CartItem cartItem = requireOwnedCartItem(memberId, cartItemId);
        cartItem.changeQuantity(quantity);
        return CartItemResponse.from(cartItem);
    }

    @Transactional
    public void removeItem(Long memberId, Long cartItemId) {
        CartItem cartItem = requireOwnedCartItem(memberId, cartItemId);
        cartItemRepository.delete(cartItem);
    }

    /** 비회원 카트에 담기. 로그인 전이라 Redis 해시에만 쌓는다. */
    public void addGuestItem(String guestId, Long bookId, int quantity) {
        String key = guestCartKey(guestId);
        redisTemplate.opsForHash().increment(key, String.valueOf(bookId), quantity);
        redisTemplate.expire(key, GUEST_CART_TTL);
    }

    /** 로그인 성공 시 비회원 카트를 회원 카트로 병합하고 Redis 키를 지운다. */
    @Transactional
    public void mergeGuestCart(Long memberId, String guestId) {
        String key = guestCartKey(guestId);
        Map<String, String> guestItems =
                redisTemplate.<String, String>opsForHash().entries(key);
        for (Map.Entry<String, String> entry : guestItems.entrySet()) {
            Long bookId = Long.valueOf(entry.getKey());
            int quantity = Integer.parseInt(entry.getValue());
            addItem(memberId, bookId, quantity);
        }
        redisTemplate.delete(key);
    }

    private String guestCartKey(String guestId) {
        return GUEST_CART_KEY_PREFIX + guestId;
    }

    private CartItem requireOwnedCartItem(Long memberId, Long cartItemId) {
        CartItem cartItem =
                cartItemRepository.findById(cartItemId).orElseThrow(() -> new CartItemNotFoundException(cartItemId));
        if (!cartItem.isOwnedBy(memberId)) {
            throw new UnauthorizedCartAccessException(cartItemId);
        }
        return cartItem;
    }
}
