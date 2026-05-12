-- =====================================================
-- V21__create_idempotency_keys.sql
-- Phase 3 — Idempotency Foundation
-- =====================================================

CREATE TABLE idempotency_keys (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(100)    NOT NULL,
    user_id         CHAR(36)        NULL,
    action_type     VARCHAR(50)     NOT NULL,
    request_hash    VARCHAR(64)     NULL,
    resource_type   VARCHAR(50)     NULL,
    resource_id     VARCHAR(100)    NULL,
    response_status INT             NULL,
    response_body   LONGTEXT        NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PROCESSING',
    error_code      VARCHAR(100)    NULL,
    expires_at      DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL,
    updated_at      DATETIME        NOT NULL,

    CONSTRAINT uk_idempotency_user_action_key
        UNIQUE (user_id, action_type, idempotency_key)
);

CREATE INDEX idx_idempotency_key       ON idempotency_keys (idempotency_key);
CREATE INDEX idx_idempotency_expires   ON idempotency_keys (expires_at);
CREATE INDEX idx_idempotency_status    ON idempotency_keys (status);
CREATE INDEX idx_idempotency_resource  ON idempotency_keys (resource_type, resource_id);
