-- =============================================================================
-- order_db — order-service 전용
--   inventory, orders, order_items, payments, deliveries, cart_items,
--   coupons, member_coupons, restock_notifications,
--   subscriptions, subscription_deliveries
--
-- db/cluster-a/03-order_db.sql (목표 스키마)을 그대로 이식한다. 커넥션이 이미
-- currentSchema=order_db 로 접속하므로 SET search_path 는 필요 없다.
-- =============================================================================

CREATE TABLE inventory (
    book_id     BIGINT PRIMARY KEY,   -- FK 없음: catalog_db.books 경계 밖
    stock       INT NOT NULL DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inventory_stock CHECK (stock >= 0)
);

CREATE TABLE coupons (
    coupon_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coupon_code          VARCHAR(100) NOT NULL,
    coupon_name          VARCHAR(255) NOT NULL,
    discount_amount      BIGINT NOT NULL,
    minimum_order_amount BIGINT NOT NULL DEFAULT 0,
    expires_at           TIMESTAMP NOT NULL,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupons_code UNIQUE (coupon_code),
    CONSTRAINT chk_coupons_discount CHECK (discount_amount > 0)
);

CREATE TABLE member_coupons (
    member_coupon_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id        BIGINT NOT NULL,  -- FK 없음: member_db 경계 밖
    coupon_id        BIGINT NOT NULL,
    is_used          BOOLEAN NOT NULL DEFAULT FALSE,
    used_at          TIMESTAMP NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_coupons_coupon FOREIGN KEY (coupon_id)
        REFERENCES coupons (coupon_id) ON DELETE RESTRICT,
    CONSTRAINT uk_member_coupons UNIQUE (member_id, coupon_id)
);

CREATE TABLE cart_items (
    cart_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id    BIGINT NOT NULL,  -- FK 없음: member_db 경계 밖
    book_id      BIGINT NOT NULL,  -- FK 없음: catalog_db 경계 밖
    quantity     INT NOT NULL DEFAULT 1,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_items_member_book UNIQUE (member_id, book_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
);

CREATE TABLE orders (
    order_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id              BIGINT NOT NULL,  -- FK 없음: member_db 경계 밖
    address_id             BIGINT NULL,      -- FK 없음. 아래 배송지 칼럼이 주문 시점 스냅샷이다
    member_coupon_id       BIGINT NULL,
    recipient_name         VARCHAR(100) NOT NULL,
    recipient_phone        VARCHAR(30)  NOT NULL,
    postal_code            VARCHAR(20)  NOT NULL,
    address                VARCHAR(255) NOT NULL,
    address_detail         VARCHAR(255) NULL,
    items_subtotal         BIGINT NOT NULL DEFAULT 0,
    total_amount           BIGINT NOT NULL,
    coupon_discount_amount BIGINT NOT NULL DEFAULT 0,
    used_point              BIGINT NOT NULL DEFAULT 0,
    order_status           VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_member_coupon FOREIGN KEY (member_coupon_id)
        REFERENCES member_coupons (member_coupon_id) ON DELETE SET NULL,
    CONSTRAINT chk_orders_status CHECK (order_status IN (
        'PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED', 'PREPARING', 'SHIPPED', 'DELIVERED',
        'CANCEL_REQUESTED', 'CANCELLED', 'EXCHANGE_REQUESTED', 'EXCHANGED',
        'RETURN_REQUESTED', 'RETURNED', 'REFUND_REQUESTED', 'REFUNDED')),
    CONSTRAINT chk_orders_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_orders_discount CHECK (coupon_discount_amount >= 0 AND used_point >= 0),
    CONSTRAINT chk_orders_subtotal CHECK (items_subtotal >= 0),
    CONSTRAINT chk_orders_total
        CHECK (total_amount = items_subtotal - coupon_discount_amount - used_point)
);

CREATE TABLE order_items (
    order_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    book_id       BIGINT NOT NULL,   -- FK 없음: catalog_db 경계 밖
    book_title    VARCHAR(200) NULL,
    quantity      INT NOT NULL,
    unit_price    BIGINT NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0)
);

CREATE TABLE payments (
    payment_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    card_id         BIGINT NULL,      -- FK 없음: member_db.cards 경계 밖
    payment_method  VARCHAR(10) NOT NULL DEFAULT 'CARD',
    pg_tid          VARCHAR(100) NULL,
    approval_number VARCHAR(50) NULL,
    amount          BIGINT NOT NULL,
    payment_status  VARCHAR(10) NOT NULL,
    decline_reason  VARCHAR(500) NULL,
    idempotency_key VARCHAR(64) NULL,
    approved_at     TIMESTAMP NULL,
    cancelled_at    TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_approval_number UNIQUE (approval_number),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE RESTRICT,
    CONSTRAINT chk_payments_method CHECK (payment_method IN ('CARD', 'KAKAOPAY')),
    CONSTRAINT chk_payments_status CHECK (payment_status IN ('APPROVED', 'DECLINED', 'CANCELLED')),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
);

CREATE TABLE deliveries (
    delivery_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    courier_company VARCHAR(100) NULL,
    tracking_number VARCHAR(100) NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'READY',
    shipped_at      TIMESTAMP NULL,
    delivered_at    TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT uk_deliveries_order UNIQUE (order_id),
    CONSTRAINT uk_deliveries_tracking UNIQUE (courier_company, tracking_number),
    CONSTRAINT chk_deliveries_status CHECK (delivery_status IN
        ('READY', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED'))
);

CREATE TABLE restock_notifications (
    restock_notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id               BIGINT NOT NULL,  -- FK 없음: member_db 경계 밖
    book_id                 BIGINT NOT NULL,  -- FK 없음: catalog_db 경계 밖
    is_notified             BOOLEAN NOT NULL DEFAULT FALSE,
    requested_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified_at             TIMESTAMP NULL,
    CONSTRAINT uk_restock_member_book UNIQUE (member_id, book_id)
);

CREATE TABLE subscriptions (
    subscription_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id           BIGINT NOT NULL,  -- FK 없음: member_db 경계 밖
    plan_name           VARCHAR(100) NOT NULL,
    monthly_price       BIGINT NOT NULL,
    subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    next_delivery_date  DATE NULL,
    cancelled_at        TIMESTAMP NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscriptions_status CHECK (subscription_status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT chk_subscriptions_price CHECK (monthly_price >= 0)
);

CREATE TABLE subscription_deliveries (
    subscription_delivery_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id          BIGINT NOT NULL,
    book_id                  BIGINT NOT NULL,  -- FK 없음: catalog_db 경계 밖
    delivery_round           INT NOT NULL,
    courier_company          VARCHAR(100) NULL,
    tracking_number          VARCHAR(100) NULL,
    delivery_status          VARCHAR(20) NOT NULL DEFAULT 'READY',
    shipped_at                TIMESTAMP NULL,
    delivered_at              TIMESTAMP NULL,
    created_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_deliveries_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions (subscription_id) ON DELETE CASCADE,
    CONSTRAINT uk_sub_deliveries_round UNIQUE (subscription_id, delivery_round),
    CONSTRAINT uk_sub_deliveries_tracking UNIQUE (courier_company, tracking_number),
    CONSTRAINT chk_sub_deliveries_status CHECK (delivery_status IN
        ('READY', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED'))
);

CREATE INDEX idx_orders_member_created      ON orders (member_id, created_at DESC);
CREATE INDEX idx_member_coupons_member_used ON member_coupons (member_id, is_used);
