package com.bookeatinglion.order.domain;

import com.bookeatinglion.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * db/cluster-a/03-order_db.sql 의 orders 테이블과 1:1.
 *
 * memberCouponId 는 order_db 안에서는 실제 FK(fk_orders_member_coupon)지만, 이 저장소의
 * 기존 컨벤션(Delivery.orderId 등)을 따라 JPA 연관관계가 아니라 평범한 Long 으로 둔다 —
 * 서비스 계층에서 명시적으로 MemberCouponRepository 를 조회해 검증하는 편이 지연 로딩
 * 함정 없이 더 명확하다. order_items 도 같은 이유로 별도 테이블/리포지토리로 다룬다
 * (Order 가 @OneToMany 로 들고 있지 않음).
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    private Long addressId;

    private Long memberCouponId;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String address;

    private String addressDetail;

    @Column(nullable = false)
    private long itemsSubtotal;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false)
    private long couponDiscountAmount;

    @Column(nullable = false)
    private long usedPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    private Order(
            Long memberId,
            Long addressId,
            Long memberCouponId,
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address,
            String addressDetail,
            long itemsSubtotal,
            long couponDiscountAmount,
            long usedPoint) {
        this.memberId = memberId;
        this.addressId = addressId;
        this.memberCouponId = memberCouponId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.itemsSubtotal = itemsSubtotal;
        this.couponDiscountAmount = couponDiscountAmount;
        this.usedPoint = usedPoint;
        // chk_orders_total 을 애플리케이션 레벨에서도 항상 만족시킨다.
        this.totalAmount = itemsSubtotal - couponDiscountAmount - usedPoint;
        this.orderStatus = OrderStatus.PENDING_PAYMENT;
    }

    public static Order create(
            Long memberId,
            Long addressId,
            Long memberCouponId,
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address,
            String addressDetail,
            long itemsSubtotal,
            long couponDiscountAmount,
            long usedPoint) {
        if (itemsSubtotal < couponDiscountAmount + usedPoint) {
            throw new IllegalArgumentException("할인·포인트 합계가 상품 금액을 초과할 수 없습니다: itemsSubtotal=" + itemsSubtotal);
        }
        return new Order(
                memberId,
                addressId,
                memberCouponId,
                recipientName,
                recipientPhone,
                postalCode,
                address,
                addressDetail,
                itemsSubtotal,
                couponDiscountAmount,
                usedPoint);
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void changeStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}
