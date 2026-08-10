package com.bookeatinglion.order.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookeatinglion.order.coupon.service.CouponService;
import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.domain.OrderItem;
import com.bookeatinglion.order.domain.OrderStatus;
import com.bookeatinglion.order.event.OrderPaymentCompletedPublisher;
import com.bookeatinglion.order.exception.UnauthorizedOrderAccessException;
import com.bookeatinglion.order.inventory.domain.Inventory;
import com.bookeatinglion.order.inventory.repository.InventoryRepository;
import com.bookeatinglion.order.payment.domain.Payment;
import com.bookeatinglion.order.payment.domain.PaymentMethod;
import com.bookeatinglion.order.payment.dto.PaymentApproveRequest;
import com.bookeatinglion.order.payment.dto.PaymentCancelRequest;
import com.bookeatinglion.order.payment.dto.PaymentResponse;
import com.bookeatinglion.order.payment.exception.InvalidOrderStatusForPaymentException;
import com.bookeatinglion.order.payment.exception.PaymentNotFoundException;
import com.bookeatinglion.order.payment.repository.PaymentRepository;
import com.bookeatinglion.order.repository.OrderItemRepository;
import com.bookeatinglion.order.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderPaymentCompletedPublisher paymentCompletedPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Order pendingOrder(Long memberId) {
        Order order = Order.create(memberId, null, null, "홍길동", "010", "12345", "서울시", null, 20_000, 0, 0);
        ReflectionTestUtils.setField(order, "id", 100L);
        return order;
    }

    @Test
    void 결제를_승인하면_주문상태가_PAID로_바뀌고_이벤트를_발행한다() {
        Order order = pendingOrder(1L);
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(new OrderItem(100L, 1L, "책", 2, 10_000)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentApproveRequest request = new PaymentApproveRequest(100L, PaymentMethod.CARD, null, "idem-1");
        PaymentResponse response = paymentService.approve(1L, request);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.approvalNumber()).startsWith("APP");
        verify(paymentCompletedPublisher).publish(any());
    }

    @Test
    void 같은_idempotencyKey로_재요청하면_기존_결제를_그대로_반환한다() {
        Payment existing = Payment.approve(100L, null, PaymentMethod.CARD, "PG1", "APP1", 20_000, "idem-1");
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentApproveRequest request = new PaymentApproveRequest(100L, PaymentMethod.CARD, null, "idem-1");
        PaymentResponse response = paymentService.approve(1L, request);

        assertThat(response.paymentId()).isEqualTo(1L);
        verify(orderRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 결제대기가_아닌_주문은_승인할_수_없다() {
        Order order = pendingOrder(1L);
        order.changeStatus(OrderStatus.PAID);
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentApproveRequest request = new PaymentApproveRequest(100L, PaymentMethod.CARD, null, "idem-1");

        assertThatThrownBy(() -> paymentService.approve(1L, request))
                .isInstanceOf(InvalidOrderStatusForPaymentException.class);
    }

    @Test
    void 타인의_주문은_결제할_수_없다() {
        Order order = pendingOrder(2L);
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentApproveRequest request = new PaymentApproveRequest(100L, PaymentMethod.CARD, null, "idem-1");

        assertThatThrownBy(() -> paymentService.approve(1L, request))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    void 결제를_취소하면_주문취소_재고복원_쿠폰복원이_한번에_처리된다() {
        Order order = Order.create(1L, null, 5L, "홍길동", "010", "12345", "서울시", null, 20_000, 1_000, 0);
        ReflectionTestUtils.setField(order, "id", 100L);
        Payment payment = Payment.approve(100L, null, PaymentMethod.CARD, "PG1", "APP1", 19_000, "idem-1");
        ReflectionTestUtils.setField(payment, "id", 1L);
        Inventory inventory = new Inventory(1L, 8);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(new OrderItem(100L, 1L, "책", 2, 10_000)));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        paymentService.cancel(1L, 1L, new PaymentCancelRequest("단순 변심"));

        assertThat(payment.isCancelled()).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(inventory.getStock()).isEqualTo(10);
        verify(couponService, times(1)).restore(5L);
    }

    @Test
    void 존재하지_않는_결제를_취소하면_예외를_던진다() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancel(1L, 999L, new PaymentCancelRequest("사유")))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
