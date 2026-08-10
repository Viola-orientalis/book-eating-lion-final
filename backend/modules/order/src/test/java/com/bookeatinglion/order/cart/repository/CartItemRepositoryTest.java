package com.bookeatinglion.order.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookeatinglion.order.OrderModuleTestApplication;
import com.bookeatinglion.order.cart.domain.CartItem;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = OrderModuleTestApplication.class)
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void 회원의_장바구니를_조회한다() {
        cartItemRepository.save(new CartItem(1L, 10L, 2));
        cartItemRepository.save(new CartItem(1L, 20L, 1));
        cartItemRepository.save(new CartItem(2L, 10L, 5));

        List<CartItem> items = cartItemRepository.findByMemberId(1L);

        assertThat(items).hasSize(2);
    }

    @Test
    void memberId와_bookId로_단건_조회한다() {
        cartItemRepository.save(new CartItem(1L, 10L, 2));

        assertThat(cartItemRepository.findByMemberIdAndBookId(1L, 10L)).isPresent();
        assertThat(cartItemRepository.findByMemberIdAndBookId(1L, 999L)).isEmpty();
    }

    @Test
    void 지정한_ID와_회원의_항목만_삭제한다() {
        CartItem mine = cartItemRepository.save(new CartItem(1L, 10L, 2));
        CartItem others = cartItemRepository.save(new CartItem(2L, 10L, 2));

        cartItemRepository.deleteByIdInAndMemberId(List.of(mine.getId(), others.getId()), 1L);

        assertThat(cartItemRepository.findById(mine.getId())).isEmpty();
        assertThat(cartItemRepository.findById(others.getId())).isPresent();
    }
}
