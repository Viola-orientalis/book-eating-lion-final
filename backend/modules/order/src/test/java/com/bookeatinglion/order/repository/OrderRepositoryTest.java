package com.bookeatinglion.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookeatinglion.order.OrderModuleTestApplication;
import com.bookeatinglion.order.domain.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = OrderModuleTestApplication.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void 회원의_주문을_최신순으로_페이징_조회한다() {
        orderRepository.save(Order.create(1L, null, null, "홍길동", "010", "12345", "서울시", null, 10_000, 0, 0));
        orderRepository.save(Order.create(1L, null, null, "홍길동", "010", "12345", "서울시", null, 20_000, 0, 0));
        orderRepository.save(Order.create(2L, null, null, "김철수", "010", "54321", "부산시", null, 5_000, 0, 0));

        Page<Order> page = orderRepository.findByMemberIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(order -> order.getMemberId().equals(1L));
    }
}
