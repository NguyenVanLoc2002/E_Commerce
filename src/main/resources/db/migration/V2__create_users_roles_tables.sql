-- =====================================================
-- V2__create_users_roles_tables.sql
-- Auth & User domain — Phase 1
-- =====================================================

-- ─── Roles ──────────────────────────────────────────────────────────────────

CREATE TABLE roles (
    id          CHAR(36) PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL,
    created_by  VARCHAR(100) NULL,
    updated_at  DATETIME     NOT NULL,
    updated_by  VARCHAR(100) NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- ─── Users ──────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id              CHAR(36) PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NULL,
    last_name       VARCHAR(100) NULL,
    phone_number    VARCHAR(20)  NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at   DATETIME     NULL,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME     NULL,
    deleted_by      VARCHAR(100) NULL,
    created_at      DATETIME     NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_at      DATETIME     NOT NULL,
    updated_by      VARCHAR(100) NULL,
    CONSTRAINT uq_users_email        UNIQUE (email),
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number)
);

-- ─── User-Roles junction ─────────────────────────────────────────────────────

CREATE TABLE user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    CONSTRAINT pk_user_roles     PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ─── Indexes ────────────────────────────────────────────────────────────────

CREATE INDEX idx_users_status       ON users(status);
CREATE INDEX idx_users_created_at   ON users(created_at);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- ─── Seed default roles ─────────────────────────────────────────────────────

INSERT INTO roles (id, name, description, created_at, created_by, updated_at, updated_by)
VALUES
    (UUID(), 'SUPER_ADMIN', 'Super administrator with full system access',    NOW(), 'system', NOW(), 'system'),
    (UUID(), 'ADMIN',       'Administrator with management access',           NOW(), 'system', NOW(), 'system'),
    (UUID(), 'STAFF',       'Staff member with limited management access',   NOW(), 'system', NOW(), 'system'),
    (UUID(), 'CUSTOMER',    'Registered customer',                            NOW(), 'system', NOW(), 'system');
