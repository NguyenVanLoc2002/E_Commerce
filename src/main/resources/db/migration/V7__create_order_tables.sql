-- =====================================================
-- V7__create_order_tables.sql
-- Phase 5 — Order module
-- =====================================================

-- ─── Orders ────────────────────────────────────────────────────────────────
-- Shipping address is snapshotted (not referenced) to preserve order history
-- even if the customer modifies or soft-deletes their address later.

CREATE TABLE orders (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id         BIGINT         NOT NULL,
    order_code          VARCHAR(50)    NOT NULL,
    status              VARCHAR(50)    NOT NULL DEFAULT 'PENDING',

    -- Shipping address snapshot
    receiver_name       VARCHAR(100)   NOT NULL,
    receiver_phone      VARCHAR(20)    NOT NULL,
    shipping_street     VARCHAR(255)   NOT NULL,
    shipping_ward       VARCHAR(100)   NOT NULL,
    shipping_district   VARCHAR(100)   NOT NULL,
    shipping_city       VARCHAR(100)   NOT NULL,
    shipping_postal_code VARCHAR(20),

    -- Pricing
    sub_total           DECIMAL(18,2) NOT NULL,
    discount_amount     DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    shipping_fee        DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(18,2) NOT NULL,

    -- Payment
    payment_method      VARCHAR(50)    NOT NULL DEFAULT 'COD',
    payment_status      VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    paid_at             DATETIME       NULL,

    -- Voucher
    voucher_code        VARCHAR(100)   NULL,

    -- Notes
    customer_note       VARCHAR(500)   NULL,
    admin_note          VARCHAR(500)   NULL,

    created_at          DATETIME       NOT NULL,
    created_by          VARCHAR(100)   NULL,
    updated_at          DATETIME       NOT NULL,
    updated_by          VARCHAR(100)   NULL,

    CONSTRAINT fk_order_customer   FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT uq_orders_code      UNIQUE (order_code)
);

CREATE INDEX idx_orders_customer_id    ON orders(customer_id);
CREATE INDEX idx_orders_status         ON orders(status);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_created_at     ON orders(created_at);

-- ─── Order Items ────────────────────────────────────────────────────────────
-- Snapshots product/variant data at checkout time.
-- Never depends on live product data.

CREATE TABLE order_items (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT         NOT NULL,
    variant_id      BIGINT         NOT NULL,

    -- Product snapshot
    product_name    VARCHAR(255)   NOT NULL,
    variant_name    VARCHAR(255)   NOT NULL,
    sku             VARCHAR(100)   NOT NULL,

    -- Pricing snapshot
    unit_price      DECIMAL(18,2)  NOT NULL,
    sale_price      DECIMAL(18,2)  NULL,
    quantity        INT            NOT NULL,
    line_total      DECIMAL(18,2)  NOT NULL,

    created_at      DATETIME       NOT NULL,
    updated_at      DATETIME       NOT NULL,

    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_oi_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE INDEX idx_oi_order_id   ON order_items(order_id);
CREATE INDEX idx_oi_variant_id ON order_items(variant_id);
