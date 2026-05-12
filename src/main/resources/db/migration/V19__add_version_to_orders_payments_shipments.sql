-- =====================================================
-- V19__add_version_to_orders_payments_shipments.sql
-- Phase 2 Concurrency — optimistic locking version columns
-- for high-concurrency mutable aggregates.
--
-- Orders, Payments, and Shipments are updated by both
-- customer-facing and admin endpoints. Without a version
-- column, concurrent writes silently overwrite each other
-- (lost update). These columns let JPA detect conflicts
-- and surface them as 409 Conflict to the caller.
-- =====================================================

ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE shipments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
