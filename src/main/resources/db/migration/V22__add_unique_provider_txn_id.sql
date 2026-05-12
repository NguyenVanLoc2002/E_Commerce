-- =====================================================
-- V22__add_unique_provider_txn_id.sql
-- Phase 3 — Payment Callback Idempotency
-- =====================================================
-- DB-level uniqueness on provider_txn_id prevents duplicate gateway
-- events from creating multiple PaymentTransaction rows, complementing
-- the application-level duplicate check already in processCallback.
-- NULLs remain allowed (COD/INITIATED transactions have no provider ID).

ALTER TABLE payment_transactions
    ADD CONSTRAINT uq_pt_provider_txn_id UNIQUE (provider_txn_id);
