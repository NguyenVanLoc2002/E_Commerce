-- =====================================================
-- V6__create_cart_tables.sql
-- Phase 4 — Cart module
-- =====================================================

-- ─── Carts (one per customer) ──────────────────────────────────────────────
-- Each customer has exactly one active cart.
-- When an order is placed, the cart is "checked out" and can be reused.

CREATE TABLE carts (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT       NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT uq_carts_customer_active UNIQUE (customer_id)
);

CREATE INDEX idx_carts_customer_id ON carts(customer_id);

-- ─── Cart Items ─────────────────────────────────────────────────────────────
-- Each line item references a product variant with quantity.
-- Unit price is NOT stored here — always read from the variant at checkout time.

CREATE TABLE cart_items (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id     BIGINT       NOT NULL,
    variant_id  BIGINT       NOT NULL,
    quantity    INT          NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    CONSTRAINT fk_ci_cart    FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_ci_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, variant_id)
);

CREATE INDEX idx_ci_cart_id    ON cart_items(cart_id);
CREATE INDEX idx_ci_variant_id ON cart_items(variant_id);
