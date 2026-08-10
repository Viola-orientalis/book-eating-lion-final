package com.bookeatinglion.order.coupon.domain;

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

/** member_coupons 는 created_at 만 있고 updated_at 이 없어 BaseEntity 를 상속하지 않는다. */
@Entity
@Table(name = "member_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private boolean isUsed;

    private LocalDateTime usedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public MemberCoupon(Long memberId, Long couponId) {
        this.memberId = memberId;
        this.couponId = couponId;
        this.isUsed = false;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    /** 주문 생성 시 쿠폰을 예약(소비)한다 — 재고 차감과 같은 트랜잭션에서 함께 커밋된다. */
    public void use() {
        if (this.isUsed) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다: memberCouponId=" + id);
        }
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    /** 결제 취소·환불 시 쿠폰 사용 상태를 원복한다. */
    public void restore() {
        if (!this.isUsed) {
            throw new IllegalStateException("사용되지 않은 쿠폰은 원복할 수 없습니다: memberCouponId=" + id);
        }
        this.isUsed = false;
        this.usedAt = null;
    }
}
