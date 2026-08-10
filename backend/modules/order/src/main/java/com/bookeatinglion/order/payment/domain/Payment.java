package com.bookeatinglion.order.payment.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * payments 테이블과 1:1. card_id 는 member_db.cards 경계 밖(FK 없음)이라 검증하지 않는다 —
 * 카드 승인은 실제 카드사/PG 연동 대신 로컬에서 승인번호·PG TID 를 생성하는 모킹이다.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    private Long cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private String pgTid;

    private String approvalNumber;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    private String declineReason;

    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime approvedAt;

    private LocalDateTime cancelledAt;

    private Payment(
            Long orderId,
            Long cardId,
            PaymentMethod paymentMethod,
            String pgTid,
            String approvalNumber,
            long amount,
            String idempotencyKey) {
        this.orderId = orderId;
        this.cardId = cardId;
        this.paymentMethod = paymentMethod;
        this.pgTid = pgTid;
        this.approvalNumber = approvalNumber;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.paymentStatus = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    /** 가상 카드 승인 모킹. 실제 PG 대신 로컬에서 승인번호/PG TID 를 발급한다. */
    public static Payment approve(
            Long orderId,
            Long cardId,
            PaymentMethod paymentMethod,
            String pgTid,
            String approvalNumber,
            long amount,
            String idempotencyKey) {
        return new Payment(orderId, cardId, paymentMethod, pgTid, approvalNumber, amount, idempotencyKey);
    }

    public void cancel(String reason) {
        if (this.paymentStatus == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 결제입니다: paymentId=" + id);
        }
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.declineReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isCancelled() {
        return this.paymentStatus == PaymentStatus.CANCELLED;
    }
}
