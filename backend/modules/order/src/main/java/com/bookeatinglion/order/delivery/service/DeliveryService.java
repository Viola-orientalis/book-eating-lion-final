package com.bookeatinglion.order.delivery.service;

import com.bookeatinglion.order.delivery.domain.Delivery;
import com.bookeatinglion.order.delivery.dto.DeliveryResponse;
import com.bookeatinglion.order.delivery.dto.DeliveryStatusUpdateRequest;
import com.bookeatinglion.order.delivery.exception.DeliveryNotFoundException;
import com.bookeatinglion.order.delivery.exception.UnauthorizedDeliveryAccessException;
import com.bookeatinglion.order.delivery.repository.DeliveryRepository;
import com.bookeatinglion.order.domain.Order;
import com.bookeatinglion.order.domain.OrderStatus;
import com.bookeatinglion.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 클래스가 MSA 전환에서 잘라야 했던 유일한 이음새였다(계획서 §1.2).
 *
 * 이전에는 MemberRepository 로 cognitoSub → Member 를 조회해 소유권을 검증했다.
 * member_db 는 이제 order-service 의 접근 권한 밖이므로 그 조회가 불가능하고,
 * 대신 JWT 클레임의 memberId 를 그대로 쓴다. 동기 호출을 추가한 게 아니라
 * 호출 자체가 사라진 것이다(판단 ③).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    public DeliveryResponse getDeliveryByOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new DeliveryNotFoundException(orderId));

        if (!order.getMemberId().equals(memberId)) {
            throw new UnauthorizedDeliveryAccessException(orderId);
        }

        Delivery delivery =
                deliveryRepository.findByOrderId(orderId).orElseThrow(() -> new DeliveryNotFoundException(orderId));

        return DeliveryResponse.from(delivery);
    }

    /** 배송 상태 전이. 주문 상태도 배송 상태에 맞춰(SHIPPED/DELIVERED) 함께 동기화한다. */
    @Transactional
    public DeliveryResponse updateStatus(Long memberId, Long orderId, DeliveryStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new DeliveryNotFoundException(orderId));
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedDeliveryAccessException(orderId);
        }
        Delivery delivery =
                deliveryRepository.findByOrderId(orderId).orElseThrow(() -> new DeliveryNotFoundException(orderId));

        switch (request.targetStatus()) {
            case SHIPPED -> {
                delivery.ship(request.courierCompany(), request.trackingNumber());
                order.changeStatus(OrderStatus.SHIPPED);
            }
            case IN_TRANSIT -> delivery.markInTransit();
            case DELIVERED -> {
                delivery.markDeliver();
                order.changeStatus(OrderStatus.DELIVERED);
            }
            default -> throw new IllegalStateException("지원하지 않는 배송 상태 전이입니다: " + request.targetStatus());
        }

        return DeliveryResponse.from(delivery);
    }
}
