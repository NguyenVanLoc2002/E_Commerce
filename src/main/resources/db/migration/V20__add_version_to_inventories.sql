-- =====================================================
-- V20__add_version_to_inventories.sql
-- Phase 2 Concurrency — optimistic locking version column
-- for the inventories table.
--
-- Kept as a separate migration from V19 because the
-- inventory layer already uses PESSIMISTIC_WRITE locks
-- for the critical reserve/release/complete paths and
-- has a lower immediate risk. The version column aligns
-- the schema with docs/database-guidelines.md §13 and
-- §18.3 without requiring changes to the locking strategy.
-- =====================================================

ALTER TABLE inventories
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
