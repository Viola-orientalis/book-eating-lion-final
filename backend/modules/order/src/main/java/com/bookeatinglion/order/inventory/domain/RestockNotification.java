package com.bookeatinglion.order.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * restock_notifications 매핑. requested_at/notified_at 이 BaseEntity의 created_at/updated_at 과
 * 이름·의미가 달라 상속하지 않는다. 엔티티만 — 이번 스코프에 재입고 알림 서비스는 없다.
 */
@Entity
@Table(name = "restock_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RestockNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private boolean isNotified;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime notifiedAt;
}
