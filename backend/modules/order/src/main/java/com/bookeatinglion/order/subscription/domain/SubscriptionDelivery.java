package com.bookeatinglion.order.subscription.domain;

import com.bookeatinglion.common.domain.BaseEntity;
import com.bookeatinglion.order.delivery.domain.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** subscription_deliveries 매핑. 엔티티만 — Subscription과 같은 이유로 서비스는 만들지 않는다. */
@Entity
@Table(name = "subscription_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int deliveryRound;

    private String courierCompany;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;
}
