-- ════════════════════════════════════════════════════════════
-- V002: Platform-level Zalo ZNS credential store
--
-- Holds the single platform Official Account ("Tappy Việt Nam") used to send
-- password-reset OTP. This is a MASTER table (tenant-agnostic, NO RLS) because
-- the forgot-password flow runs with no tenant context.
--
-- app_secret / access_token / refresh_token are stored AES-encrypted by the
-- application layer (EncryptedStringConverter) — the column is plain TEXT and
-- carries the `{enc}...` ciphertext, exactly like shop_integrations.config_json.
--
-- token_expiry lets PlatformZaloTokenService refresh ahead of expiry; the
-- refresh_token rotates on every refresh (single-use) and is written back here.
-- ════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS zalo_zns_credential (
    id            BIGSERIAL    PRIMARY KEY,
    app_id        VARCHAR(50)  NOT NULL,
    app_secret    TEXT         NOT NULL,
    access_token  TEXT         DEFAULT NULL,
    refresh_token TEXT         DEFAULT NULL,
    token_expiry  TIMESTAMP    DEFAULT NULL,
    oa_name       VARCHAR(255) DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    DEFAULT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP    DEFAULT NULL,
    -- One platform OA → one row. Guards against duplicate bootstrap inserts under
    -- concurrency (a pessimistic row lock can't lock a not-yet-existing row).
    CONSTRAINT uq_zalo_zns_credential_app_id UNIQUE (app_id)
);
