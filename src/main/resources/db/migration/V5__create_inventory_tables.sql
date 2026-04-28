-- =====================================================
-- V5__create_inventory_tables.sql
-- Phase 3 — Inventory module
-- =====================================================

-- ─── Warehouses ──────────────────────────────────────────────────────────────

CREATE TABLE warehouses
(
    id         CHAR(36) PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    location   VARCHAR(255) NULL,
    status     VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME     NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_at DATETIME     NOT NULL,
    updated_by VARCHAR(100) NULL,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at DATETIME     NULL,
    deleted_by VARCHAR(100) NULL,
    CONSTRAINT uq_warehouses_code UNIQUE (code)
);

CREATE INDEX idx_warehouses_status ON warehouses (status);

-- ─── Inventories (per variant per warehouse) ───────────────────────────────────────
-- available is NOT a stored column — always computed as: on_hand - reserved
-- Atomic update WHERE available >= :quantity prevents oversell.

CREATE TABLE inventories
(
    id           CHAR(36) PRIMARY KEY,
    variant_id   CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    on_hand      INT      NOT NULL DEFAULT 0,
    reserved     INT      NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    created_by   VARCHAR(100) NULL,
    updated_by   VARCHAR(100) NULL,
    CONSTRAINT uq_inventories_variant_warehouse UNIQUE (variant_id, warehouse_id),
    CONSTRAINT fk_inv_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_inv_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE INDEX idx_inv_variant_id ON inventories (variant_id);
CREATE INDEX idx_inv_warehouse_id ON inventories (warehouse_id);

-- ─── Inventory Reservations (hold stock for orders) ───────────────────────────────
-- Created at checkout. Released on cancel/complete.
-- Auto-released when expires_at passes.

CREATE TABLE inventory_reservations
(
    id             CHAR(36) PRIMARY KEY,
    variant_id     CHAR(36)    NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id   VARCHAR(100) NULL,
    quantity       INT         NOT NULL,
    status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expires_at     DATETIME NULL,
    created_at     DATETIME    NOT NULL,
    created_by     VARCHAR(100) NULL,
    updated_at     DATETIME    NOT NULL,
    updated_by     VARCHAR(100) NULL,
    CONSTRAINT fk_ir_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id)
);

CREATE INDEX idx_ir_variant_id ON inventory_reservations (variant_id);
CREATE INDEX idx_ir_status ON inventory_reservations (status);
CREATE INDEX idx_ir_expires ON inventory_reservations (expires_at);

-- ─── Stock Movements (audit trail for every inventory change) ──────────────────────
-- Records before/after values for full traceability.

CREATE TABLE stock_movements
(
    id               CHAR(36) PRIMARY KEY,
    variant_id       CHAR(36)    NOT NULL,
    warehouse_id     CHAR(36)    NOT NULL,
    movement_type    VARCHAR(50) NOT NULL,
    quantity         INT         NOT NULL,
    reference_type   VARCHAR(50) NULL,
    reference_id     VARCHAR(100) NULL,
    note             VARCHAR(500) NULL,
    before_on_hand   INT         NOT NULL DEFAULT 0,
    before_reserved  INT         NOT NULL DEFAULT 0,
    before_available INT         NOT NULL DEFAULT 0,
    after_on_hand    INT         NOT NULL DEFAULT 0,
    after_reserved   INT         NOT NULL DEFAULT 0,
    after_available  INT         NOT NULL DEFAULT 0,
    created_at       DATETIME    NOT NULL,
    created_by       VARCHAR(100) NULL,
    updated_at       DATETIME    NOT NULL,
    updated_by       VARCHAR(100) NULL,
    CONSTRAINT fk_sm_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_sm_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE INDEX idx_sm_variant_id ON stock_movements (variant_id);
CREATE INDEX idx_sm_movement_type ON stock_movements (movement_type);
CREATE INDEX idx_sm_warehouse_id ON stock_movements (warehouse_id);
CREATE INDEX idx_sm_reference ON stock_movements (reference_type, reference_id);
