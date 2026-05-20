-- =====================================================
-- V30__create_payment_configs.sql
-- Payment provider configuration managed from admin UI
-- =====================================================

CREATE TABLE payment_configs (
    id                                  CHAR(36)        NOT NULL PRIMARY KEY,
    provider                            VARCHAR(50)     NOT NULL,
    enabled                             BIT             NOT NULL DEFAULT b'0',
    environment                         VARCHAR(50)     NULL,

    momo_partner_code_enc               VARCHAR(1000)   NULL,
    momo_access_key_enc                 VARCHAR(1000)   NULL,
    momo_secret_key_enc                 VARCHAR(1000)   NULL,
    momo_create_url                     VARCHAR(500)    NULL,
    momo_redirect_url                   VARCHAR(500)    NULL,
    momo_ipn_url                        VARCHAR(500)    NULL,
    momo_request_type                   VARCHAR(100)    NULL,
    momo_lang                           VARCHAR(20)     NULL,

    paypal_client_id_enc                VARCHAR(1000)   NULL,
    paypal_client_secret_enc            VARCHAR(1000)   NULL,
    paypal_base_url                     VARCHAR(500)    NULL,
    paypal_return_url                   VARCHAR(500)    NULL,
    paypal_cancel_url                   VARCHAR(500)    NULL,
    paypal_webhook_id                   VARCHAR(255)    NULL,
    paypal_currency                     VARCHAR(20)     NULL,
    paypal_brand_name                   VARCHAR(200)    NULL,
    paypal_locale                       VARCHAR(50)     NULL,
    paypal_user_action                  VARCHAR(100)    NULL,
    paypal_payment_method_preference    VARCHAR(100)    NULL,
    paypal_shipping_preference          VARCHAR(100)    NULL,
    paypal_test_conversion_enabled      BIT             NOT NULL DEFAULT b'0',
    paypal_test_conversion_rate_vnd_usd DECIMAL(18,6)   NULL,

    connect_timeout_ms                  INT             NULL,
    read_timeout_ms                     INT             NULL,
    config_json                         TEXT            NULL,

    created_at                          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                                         ON UPDATE CURRENT_TIMESTAMP(6),
    created_by                          VARCHAR(100)    NULL,
    updated_by                          VARCHAR(100)    NULL,

    CONSTRAINT uq_payment_configs_provider UNIQUE (provider)
);

CREATE INDEX idx_payment_configs_enabled ON payment_configs(enabled);
