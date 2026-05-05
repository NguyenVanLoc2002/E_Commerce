-- =====================================================
-- V17__product_fulltext_search.sql
-- Product keyword search hardening:
--   1. Add denormalized products.search_text column (lowercased, accent-stripped).
--   2. Add MariaDB FULLTEXT index on (name, slug, search_text)
--      for IN BOOLEAN MODE search.
--   3. Add filter indexes used together with keyword search:
--      - products(is_deleted, status, is_featured, brand_id, created_at)
--      - product_categories(category_id, product_id)  (join + soft-delete pruning)
--      - product_variants(product_id, is_deleted)     (price subquery join)
--
-- Notes:
--   - search_text is internal — never exposed in public API responses.
--   - Existing rows must be re-indexed after deploy:
--       POST /api/v1/admin/products/search/reindex
--     or by re-saving each product. New rows are populated by the service layer.
-- =====================================================

-- ─── Denormalized search column ──────────────────────────────────────────────

ALTER TABLE products
    ADD COLUMN search_text TEXT NULL AFTER description;

-- ─── FULLTEXT index ──────────────────────────────────────────────────────────
-- MariaDB requires all FULLTEXT-indexed columns to use the same charset/collation.
-- products.name / products.slug are VARCHAR; search_text is TEXT — same family.
CREATE FULLTEXT INDEX ftx_products_search
    ON products (name, slug, search_text);

-- ─── Filter indexes ──────────────────────────────────────────────────────────

-- Common product-list path: WHERE is_deleted = 0 AND status = ? (AND is_featured = ?)
-- ORDER BY created_at DESC
CREATE INDEX idx_products_deleted_status_featured_created
    ON products(is_deleted, status, is_featured, created_at);

-- Brand filter on the public list path
CREATE INDEX idx_products_brand_deleted_status
    ON products(brand_id, is_deleted, status);

-- Category-product join: lookup products in a given category quickly.
-- The PK is (product_id, category_id), so the reverse direction needs its own index.
-- An index on (category_id) already exists from V4 (idx_pc_category_id);
-- add the composite (category_id, product_id) for covering joins.
CREATE INDEX idx_pc_category_product
    ON product_categories(category_id, product_id);

-- Price-range subquery: WHERE product_id = ? AND is_deleted = 0
CREATE INDEX idx_pv_product_deleted
    ON product_variants(product_id, is_deleted);
