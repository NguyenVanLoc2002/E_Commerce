-- =====================================================
-- V4__create_catalog_tables.sql
-- Phase 2 — Catalog module
-- =====================================================

-- ─── Categories (self-referencing hierarchy) ─────────────────────────────────

CREATE TABLE categories (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT       NULL,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    description     TEXT         NULL,
    image_url       VARCHAR(500) NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME     NULL,
    deleted_by      VARCHAR(100) NULL,
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_status    ON categories(status);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

-- ─── Brands ─────────────────────────────────────────────────────────────────

CREATE TABLE brands (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    logo_url        VARCHAR(500) NULL,
    description     TEXT         NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME     NULL,
    deleted_by      VARCHAR(100) NULL,
    CONSTRAINT uq_brands_slug UNIQUE (slug)
);

CREATE INDEX idx_brands_status ON brands(status);

-- ─── Products ───────────────────────────────────────────────────────────────

CREATE TABLE products (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(255) NOT NULL,
    short_description VARCHAR(500) NULL,
    description       TEXT         NULL,
    brand_id          BIGINT       NULL,
    status            VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    is_featured       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME     NOT NULL,
    created_by        VARCHAR(100) NULL,
    updated_at        DATETIME     NOT NULL,
    updated_by        VARCHAR(100) NULL,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at        DATETIME     NULL,
    deleted_by        VARCHAR(100) NULL,
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id)
);

CREATE INDEX idx_products_brand_id  ON products(brand_id);
CREATE INDEX idx_products_status    ON products(status);
CREATE INDEX idx_products_featured  ON products(is_featured);
CREATE INDEX idx_products_created_at ON products(created_at);

-- ─── Product-Categories (M:N) ──────────────────────────────────────────────

CREATE TABLE product_categories (
    product_id  BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_product_categories  PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_pc_product   FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_pc_category  FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_pc_category_id ON product_categories(category_id);

-- ─── Product Variants ───────────────────────────────────────────────────────

CREATE TABLE product_variants (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id      BIGINT       NOT NULL,
    sku             VARCHAR(100) NOT NULL,
    barcode         VARCHAR(100) NULL,
    variant_name    VARCHAR(255) NOT NULL,
    base_price      DECIMAL(18,2) NOT NULL,
    sale_price      DECIMAL(18,2) NULL,
    compare_at_price DECIMAL(18,2) NULL,
    weight_gram     INT          NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME     NULL,
    deleted_by      VARCHAR(100) NULL,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT fk_pv_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_pv_product_id ON product_variants(product_id);
CREATE INDEX idx_pv_status     ON product_variants(status);

-- ─── Product Attributes (size, color, material) ────────────────────────────

CREATE TABLE product_attributes (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    type        VARCHAR(50)  NOT NULL DEFAULT 'VARIANT',
    created_at  DATETIME     NOT NULL,
    created_by  VARCHAR(100) NULL,
    updated_at  DATETIME     NOT NULL,
    updated_by  VARCHAR(100) NULL,
    CONSTRAINT uq_product_attributes_code UNIQUE (code)
);

-- ─── Product Attribute Values (S, M, L, Red, Blue) ──────────────────────────

CREATE TABLE product_attribute_values (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    attribute_id    BIGINT       NOT NULL,
    value           VARCHAR(100) NOT NULL,
    display_value   VARCHAR(100) NULL,
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    CONSTRAINT fk_pav_attribute FOREIGN KEY (attribute_id) REFERENCES product_attributes(id)
);

CREATE INDEX idx_pav_attribute_id ON product_attribute_values(attribute_id);

-- ─── Variant-Attribute Values (M:N variant ↔ attribute value) ─────────────

CREATE TABLE variant_attribute_values (
    variant_id          BIGINT NOT NULL,
    attribute_value_id  BIGINT NOT NULL,
    CONSTRAINT pk_variant_attribute_values     PRIMARY KEY (variant_id, attribute_value_id),
    CONSTRAINT fk_vav_variant          FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_vav_attribute_value  FOREIGN KEY (attribute_value_id) REFERENCES product_attribute_values(id)
);

CREATE INDEX idx_vav_attribute_value_id ON variant_attribute_values(attribute_value_id);

-- ─── Product Media (images, videos per product/variant) ───────────────────

CREATE TABLE product_media (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    variant_id  BIGINT       NULL,
    media_url   VARCHAR(500) NOT NULL,
    media_type  VARCHAR(20)  NOT NULL DEFAULT 'IMAGE',
    sort_order  INT          NOT NULL DEFAULT 0,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL,
    created_by  VARCHAR(100) NULL,
    updated_at  DATETIME     NOT NULL,
    updated_by  VARCHAR(100) NULL,
    CONSTRAINT fk_pm_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_pm_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE INDEX idx_pm_product_id ON product_media(product_id);
CREATE INDEX idx_pm_variant_id ON product_media(variant_id);
