package com.bookeatinglion.order.subscription.domain;

import com.bookeatinglion.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * db/cluster-a/03-order_db.sql 의 subscriptions 테이블 매핑. Flyway/JPA 스키마 정렬을 위한
 * 엔티티만 — 이번 스코프의 Objective(cart/coupon/order/inventory/payment/delivery)에는
 * 정기구독 서비스/컨트롤러가 포함되지 않는다.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false)
    private long monthlyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus;

    private LocalDate nextDeliveryDate;

    private LocalDateTime cancelledAt;
}
