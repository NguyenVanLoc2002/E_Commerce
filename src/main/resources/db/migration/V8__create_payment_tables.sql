-- =====================================================
-- V8__create_payment_tables.sql
-- Phase 6 — Payment module
-- =====================================================

-- ─── Payments (one per order) ─────────────────────────────────────────────
-- Tracks the overall payment state for an order.
-- Order table retains denormalized payment_method/payment_status/paid_at
-- for fast querying; this table holds the full payment record.

CREATE TABLE payments (
    id              CHAR(36) PRIMARY KEY,
    order_id        CHAR(36)         NOT NULL,
    payment_code    VARCHAR(50)    NOT NULL,
    method          VARCHAR(50)    NOT NULL,
    status          VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    amount          DECIMAL(18,2)  NOT NULL,
    paid_at         DATETIME       NULL,
    expired_at      DATETIME       NULL,
    created_at      DATETIME       NOT NULL,
    created_by      VARCHAR(100)   NULL,
    updated_at      DATETIME       NOT NULL,
    updated_by      VARCHAR(100)   NULL,

    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT uq_payments_order_id  UNIQUE (order_id),
    CONSTRAINT uq_payments_code     UNIQUE (payment_code)
);

CREATE INDEX idx_payments_status     ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- ─── Payment Transactions (audit trail) ───────────────────────────────────
-- Records every attempt/state change for a payment.
-- Immutable — never updated or deleted.

CREATE TABLE payment_transactions (
    id              CHAR(36) PRIMARY KEY,
    payment_id      CHAR(36)         NOT NULL,
    transaction_code VARCHAR(50)   NOT NULL,
    status          VARCHAR(50)    NOT NULL,
    amount          DECIMAL(18,2)  NOT NULL,
    method          VARCHAR(50)    NOT NULL,
    provider        VARCHAR(100)   NULL,
    provider_txn_id VARCHAR(200)   NULL,
    reference_type  VARCHAR(50)    NULL,
    reference_id    VARCHAR(100)   NULL,
    payload         TEXT           NULL,
    note            VARCHAR(500)   NULL,
    created_at      DATETIME       NOT NULL,
    updated_at   DATETIME       NOT NULL,
    created_by      VARCHAR(100)   NULL,
    updated_by   VARCHAR(100) NULL,

    CONSTRAINT fk_pt_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT uq_pt_code    UNIQUE (transaction_code)
);

CREATE INDEX idx_pt_payment_id     ON payment_transactions(payment_id);
CREATE INDEX idx_pt_status         ON payment_transactions(status);
CREATE INDEX idx_pt_provider_txn   ON payment_transactions(provider_txn_id);
CREATE INDEX idx_pt_reference      ON payment_transactions(reference_type, reference_id);
