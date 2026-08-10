package com.bookeatinglion.order.cart.domain;

import com.bookeatinglion.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int quantity;

    public CartItem(Long memberId, Long bookId, int quantity) {
        validateQuantity(quantity);
        this.memberId = memberId;
        this.bookId = bookId;
        this.quantity = quantity;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void increaseQuantity(int delta) {
        validateQuantity(this.quantity + delta);
        this.quantity += delta;
    }

    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("장바구니 수량은 1 이상이어야 합니다: " + quantity);
        }
    }
}
