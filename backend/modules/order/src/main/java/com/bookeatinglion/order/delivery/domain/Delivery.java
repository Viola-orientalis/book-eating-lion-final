package com.bookeatinglion.order.delivery.domain;

import com.bookeatinglion.common.domain.BaseEntity;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    private String courierCompany;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @Builder
    public Delivery(Long orderId, String courierCompany, String trackingNumber, DeliveryStatus deliveryStatus) {
        this.orderId = orderId;
        this.courierCompany = courierCompany;
        this.trackingNumber = trackingNumber;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : DeliveryStatus.READY;
    }

    public void ship(String courierCompany, String trackingNumber) {
        requireStatus(DeliveryStatus.READY);
        this.courierCompany = courierCompany;
        this.trackingNumber = trackingNumber;
        this.deliveryStatus = DeliveryStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    public void markInTransit() {
        requireStatus(DeliveryStatus.SHIPPED);
        this.deliveryStatus = DeliveryStatus.IN_TRANSIT;
    }

    public void markDeliver() {
        requireStatus(DeliveryStatus.IN_TRANSIT);
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    private void requireStatus(DeliveryStatus expected) {
        if (this.deliveryStatus != expected) {
            throw new IllegalStateException(
                    "배송 상태 전이가 불가능합니다: current=" + this.deliveryStatus + ", required=" + expected);
        }
    }
}
