package com.bookeatinglion.order.payment.service;

import com.bookeatinglion.common.event.OrderPaymentCompleted;
import com.bookeatinglion.order.coupon.service.CouponService;
import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.domain.OrderItem;
import com.bookeatinglion.order.domain.OrderStatus;
import com.bookeatinglion.order.dto.OrderItemResponse;
import com.bookeatinglion.order.event.OrderPaymentCompletedPublisher;
import com.bookeatinglion.order.exception.OrderNotFoundException;
import com.bookeatinglion.order.exception.UnauthorizedOrderAccessException;
import com.bookeatinglion.order.inventory.domain.Inventory;
import com.bookeatinglion.order.inventory.repository.InventoryRepository;
import com.bookeatinglion.order.payment.domain.Payment;
import com.bookeatinglion.order.payment.dto.PaymentApproveRequest;
import com.bookeatinglion.order.payment.dto.PaymentCancelRequest;
import com.bookeatinglion.order.payment.dto.PaymentResponse;
import com.bookeatinglion.order.payment.dto.ReceiptResponse;
import com.bookeatinglion.order.payment.exception.InvalidOrderStatusForPaymentException;
import com.bookeatinglion.order.payment.exception.PaymentNotFoundException;
import com.bookeatinglion.order.payment.exception.UnauthorizedPaymentAccessException;
import com.bookeatinglion.order.payment.repository.PaymentRepository;
import com.bookeatinglion.order.repository.OrderItemRepository;
import com.bookeatinglion.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 승인/영수증/취소. "가상 카드 정보 모킹" — 실제 PG·카드사 연동 대신 로컬에서 승인번호와
 * PG TID 를 생성한다. member_db.cards 는 아직 API 가 없기도 하고, 있었어도 이 서비스는 판단 ③에
 * 따라 member-service 를 동기 호출하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final CouponService couponService;
    private final OrderPaymentCompletedPublisher paymentCompletedPublisher;

    @Transactional
    public PaymentResponse approve(Long memberId, PaymentApproveRequest request) {
        // 재시도 안전: 같은 idempotencyKey 로 이미 승인된 결제가 있으면 그걸 그대로 반환한다.
        var existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return PaymentResponse.from(existing.get());
        }

        Order order = orderRepository
                .findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException(request.orderId());
        }
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStatusForPaymentException(order.getId(), order.getOrderStatus());
        }

        String approvalNumber = "APP" + System.currentTimeMillis();
        String pgTid = "PG" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // uk_payments_idempotency 위반(동시 중복 요청 경쟁)은 여기서 잡지 않는다 — 이미 flush 된
        // 영속성 컨텍스트를 계속 쓰는 게 더 위험하다. PaymentExceptionHandler 가 409로 매핑하고,
        // 클라이언트는 같은 idempotencyKey 로 재요청하면 위 조회 분기에서 안전하게 처리된다.
        Payment payment = Payment.approve(
                order.getId(),
                request.cardId(),
                request.paymentMethod(),
                pgTid,
                approvalNumber,
                order.getTotalAmount(),
                request.idempotencyKey());
        paymentRepository.save(payment);

        order.changeStatus(OrderStatus.PAID);

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        String bookIdsCsv =
                items.stream().map(item -> String.valueOf(item.getBookId())).collect(Collectors.joining(","));
        paymentCompletedPublisher.publish(new OrderPaymentCompleted(
                order.getId(),
                memberId,
                order.getTotalAmount(),
                bookIdsCsv,
                LocalDateTime.now().toString()));

        return PaymentResponse.from(payment);
    }

    public ReceiptResponse getReceipt(Long memberId, Long paymentId) {
        Payment payment = requireOwnedPayment(memberId, paymentId);
        Order order = orderRepository
                .findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(payment.getOrderId()));
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        return ReceiptResponse.of(payment, order, items);
    }

    /** 단일 트랜잭션으로 결제 취소 + 주문 취소 + 재고 원복 + 쿠폰 원복을 전부 처리한다. */
    @Transactional
    public void cancel(Long memberId, Long paymentId, PaymentCancelRequest request) {
        Payment payment = requireOwnedPayment(memberId, paymentId);
        Order order = orderRepository
                .findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(payment.getOrderId()));

        payment.cancel(request.reason());
        order.changeStatus(OrderStatus.CANCELLED);

        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            Inventory inventory = inventoryRepository
                    .findById(item.getBookId())
                    .orElseThrow(() -> new IllegalStateException("재고 레코드 부재: bookId=" + item.getBookId()));
            inventory.restock(item.getQuantity());
        }

        if (order.getMemberCouponId() != null) {
            couponService.restore(order.getMemberCouponId());
        }
    }

    private Payment requireOwnedPayment(Long memberId, Long paymentId) {
        Payment payment =
                paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Order order = orderRepository
                .findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(payment.getOrderId()));
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedPaymentAccessException(paymentId);
        }
        return payment;
    }
}
