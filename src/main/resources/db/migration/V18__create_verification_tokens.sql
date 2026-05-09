-- =====================================================
-- V18__create_verification_tokens.sql
-- Verification token infrastructure (OTP / reset token).
--
-- One row per outstanding verification attempt:
--   * OTP value is stored as a SHA-256 hash (never plain).
--   * After successful OTP verification, an opaque reset token may
--     be issued — its SHA-256 hash is stored on the same row.
--   * `target` is normalized: email is lowercased before hashing/lookup.
--   * `target_type` documents what `target` is (EMAIL today; reserved
--     for USER and ORDER targets in later phases).
--   * `purpose` partitions the keyspace so a FORGOT_PASSWORD OTP
--     cannot be reused for VERIFY_EMAIL etc.
--
-- IDs are CHAR(36) UUIDs to match BaseEntity (matches existing schema
-- conventions — see V2__create_users_roles_tables.sql).
-- Charset / collation are intentionally left to the table default, so
-- the user_id column matches users.id (errno 150 otherwise).
-- =====================================================

CREATE TABLE verification_tokens (
    id                 CHAR(36)     NOT NULL PRIMARY KEY,
    user_id            CHAR(36)     NULL,
    target_type        VARCHAR(20)  NOT NULL,
    target             VARCHAR(255) NOT NULL,
    purpose            VARCHAR(30)  NOT NULL,
    token_hash         VARCHAR(255) NOT NULL,
    reset_token_hash   VARCHAR(255) NULL,
    expires_at         DATETIME     NOT NULL,
    verified_at        DATETIME     NULL,
    used_at            DATETIME     NULL,
    attempt_count      INT          NOT NULL DEFAULT 0,
    max_attempts       INT          NOT NULL DEFAULT 5,
    resend_count       INT          NOT NULL DEFAULT 0,
    last_sent_at       DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NULL,
    CONSTRAINT fk_vt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_vt_target_purpose    ON verification_tokens (target, purpose);
CREATE INDEX idx_vt_user_purpose      ON verification_tokens (user_id, purpose);
CREATE INDEX idx_vt_reset_token_hash  ON verification_tokens (reset_token_hash);
CREATE INDEX idx_vt_expires_at        ON verification_tokens (expires_at);
CREATE INDEX idx_vt_used_at           ON verification_tokens (used_at);
