-- =====================================================
-- V3__create_customer_address_tables.sql
-- Customer + Address domain — Phase 1
-- =====================================================

-- ─── Customers ──────────────────────────────────────────────────────────────
-- Business profile linked 1-to-1 with users (auth identity).
-- User holds auth fields (email, password, status, roles).
-- Customer holds profile fields (gender, birthDate, avatar, loyalty).

CREATE TABLE customers (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36)       NOT NULL,
    gender          VARCHAR(20)  NULL,
    birth_date      DATE         NULL,
    avatar_url      VARCHAR(500) NULL,
    loyalty_points  INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME     NULL,
    deleted_by      VARCHAR(100) NULL,
    CONSTRAINT uq_customers_user_id UNIQUE (user_id),
    CONSTRAINT fk_customers_user    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ─── Addresses ──────────────────────────────────────────────────────────────
-- Vietnamese address structure: street → ward → district → city.

CREATE TABLE addresses (
    id              CHAR(36) PRIMARY KEY,
    customer_id     CHAR(36)       NOT NULL,
    receiver_name   VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(20)  NOT NULL,
    street_address  VARCHAR(255) NOT NULL,
    ward            VARCHAR(100) NOT NULL,
    district        VARCHAR(100) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(20)  NULL,
    address_type    VARCHAR(20)  NOT NULL DEFAULT 'SHIPPING',
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    label           VARCHAR(50)  NULL,
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME     NULL,
    deleted_by      VARCHAR(100) NULL,
    CONSTRAINT fk_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- ─── Indexes ────────────────────────────────────────────────────────────────

CREATE INDEX idx_addresses_customer_id ON addresses(customer_id);
CREATE INDEX idx_addresses_is_default  ON addresses(customer_id, is_default);
CREATE INDEX idx_customers_user_id     ON customers(user_id);
