package com.bookeatinglion.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookeatinglion.order.cart.repository.CartItemRepository;
import com.bookeatinglion.order.coupon.service.CouponService;
import com.bookeatinglion.order.delivery.repository.DeliveryRepository;
import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.dto.OrderCreateRequest;
import com.bookeatinglion.order.dto.OrderItemRequest;
import com.bookeatinglion.order.dto.OrderResponse;
import com.bookeatinglion.order.exception.EmptyOrderItemsException;
import com.bookeatinglion.order.exception.LockAcquisitionException;
import com.bookeatinglion.order.exception.OrderNotFoundException;
import com.bookeatinglion.order.exception.UnauthorizedOrderAccessException;
import com.bookeatinglion.order.inventory.domain.InsufficientStockException;
import com.bookeatinglion.order.inventory.domain.Inventory;
import com.bookeatinglion.order.inventory.repository.InventoryRepository;
import com.bookeatinglion.order.repository.OrderItemRepository;
import com.bookeatinglion.order.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                inventoryRepository,
                cartItemRepository,
                deliveryRepository,
                couponService,
                redissonClient,
                transactionManager);

        // TransactionTemplate.execute() 가 실제 트랜잭션 없이도 콜백을 실행하도록 최소한만 스텁한다.
        lenientTransactionManager();
    }

    private void lenientTransactionManager() {
        org.mockito.Mockito.lenient()
                .when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
    }

    private void stubLockSucceeds() {
        RLock lock = mock(RLock.class);
        RLock multiLock = mock(RLock.class);
        org.mockito.Mockito.lenient()
                .when(redissonClient.getLock(any(String.class)))
                .thenReturn(lock);
        try {
            org.mockito.Mockito.lenient()
                    .when(multiLock.tryLock(anyLong(), anyLong(), any()))
                    .thenReturn(true);
        } catch (InterruptedException ignored) {
            // stub 설정 중에는 발생하지 않는다.
        }
        org.mockito.Mockito.lenient()
                .when(redissonClient.getMultiLock(any(RLock[].class)))
                .thenReturn(multiLock);
    }

    private OrderCreateRequest requestWithOneItem() {
        return new OrderCreateRequest(
                List.of(new OrderItemRequest(1L, "책 제목", 2, 10_000)),
                null,
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                null,
                null,
                0L,
                null);
    }

    @Test
    void 정상적으로_주문을_생성하면_재고를_차감하고_배송건을_함께_만든다() {
        stubLockSucceeds();
        Inventory inventory = new Inventory(1L, 10);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 100L);
            return order;
        });

        OrderResponse response = orderService.createOrder(1L, requestWithOneItem());

        assertThat(response.itemsSubtotal()).isEqualTo(20_000);
        assertThat(response.totalAmount()).isEqualTo(20_000);
        assertThat(inventory.getStock()).isEqualTo(8);
        verify(deliveryRepository).save(any());
        verify(orderItemRepository).saveAll(any());
    }

    @Test
    void 주문_아이템이_비어있으면_예외를_던진다() {
        OrderCreateRequest request =
                new OrderCreateRequest(List.of(), null, "홍길동", "010-1234-5678", "12345", "서울시", null, null, 0L, null);

        assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(EmptyOrderItemsException.class);
    }

    @Test
    void 재고가_부족하면_예외를_던지고_주문을_저장하지_않는다() {
        stubLockSucceeds();
        Inventory inventory = new Inventory(1L, 1);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> orderService.createOrder(1L, requestWithOneItem()))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void 락_획득에_실패하면_예외를_던진다() {
        RLock multiLock = mock(RLock.class);
        org.mockito.Mockito.lenient()
                .when(redissonClient.getLock(any(String.class)))
                .thenReturn(mock(RLock.class));
        when(redissonClient.getMultiLock(any(RLock[].class))).thenReturn(multiLock);
        try {
            when(multiLock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);
        } catch (InterruptedException ignored) {
        }

        assertThatThrownBy(() -> orderService.createOrder(1L, requestWithOneItem()))
                .isInstanceOf(LockAcquisitionException.class);
    }

    @Test
    void 존재하지_않는_주문을_조회하면_예외를_던진다() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(1L, 999L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void 타인의_주문을_조회하면_권한_예외를_던진다() {
        Order order = Order.create(2L, null, null, "홍길동", "010", "12345", "서울시", null, 10_000, 0, 0);
        ReflectionTestUtils.setField(order, "id", 100L);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(1L, 100L)).isInstanceOf(UnauthorizedOrderAccessException.class);
    }
}
