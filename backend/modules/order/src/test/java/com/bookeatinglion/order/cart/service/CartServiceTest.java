package com.bookeatinglion.order.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookeatinglion.order.cart.domain.CartItem;
import com.bookeatinglion.order.cart.dto.CartItemResponse;
import com.bookeatinglion.order.cart.exception.CartItemNotFoundException;
import com.bookeatinglion.order.cart.exception.UnauthorizedCartAccessException;
import com.bookeatinglion.order.cart.repository.CartItemRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private CartService cartService;

    private CartItem cartItem(Long memberId, Long id) {
        CartItem cartItem = new CartItem(memberId, 1L, 2);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    @Test
    void 이미_담긴_책이면_수량만_증가한다() {
        CartItem existing = cartItem(1L, 10L);
        when(cartItemRepository.findByMemberIdAndBookId(1L, 1L)).thenReturn(Optional.of(existing));

        CartItemResponse response = cartService.addItem(1L, 1L, 3);

        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    void 처음_담는_책이면_새로_생성한다() {
        when(cartItemRepository.findByMemberIdAndBookId(1L, 2L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemResponse response = cartService.addItem(1L, 2L, 1);

        assertThat(response.bookId()).isEqualTo(2L);
        assertThat(response.quantity()).isEqualTo(1);
    }

    @Test
    void 타인의_장바구니_항목은_수정할_수_없다() {
        CartItem existing = cartItem(2L, 10L);
        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.updateQuantity(1L, 10L, 5))
                .isInstanceOf(UnauthorizedCartAccessException.class);
    }

    @Test
    void 존재하지_않는_장바구니_항목을_삭제하면_예외를_던진다() {
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(1L, 999L)).isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void 비회원_장바구니를_병합하면_DB에_반영되고_Redis_키가_삭제된다() {
        @SuppressWarnings("unchecked")
        HashOperations<String, String, String> hashOperations = mock(HashOperations.class);
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("cart:guest:guest-1")).thenReturn(Map.of("1", "3"));
        when(cartItemRepository.findByMemberIdAndBookId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        cartService.mergeGuestCart(1L, "guest-1");

        verify(cartItemRepository).save(any(CartItem.class));
        verify(redisTemplate).delete("cart:guest:guest-1");
    }
}
