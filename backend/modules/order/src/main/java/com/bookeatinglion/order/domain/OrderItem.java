package com.bookeatinglion.order.domain;

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
 * order_items 는 created_at 만 있고 updated_at 이 없다(주문 시점 스냅샷이라 갱신 자체가 없음) —
 * 그래서 updated_at 까지 요구하는 {@link com.bookeatinglion.common.domain.BaseEntity} 를
 * 상속하지 않는다.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long bookId;

    /** 주문 시점 도서 스냅샷. 원본이 바뀌어도 갱신하지 않는다 — 요청 바디 값을 그대로 저장한다. */
    private String bookTitle;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long unitPrice;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public OrderItem(Long orderId, Long bookId, String bookTitle, int quantity, long unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다: " + quantity);
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다: " + unitPrice);
        }
        this.orderId = orderId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public long subtotal() {
        return unitPrice * quantity;
    }
}
