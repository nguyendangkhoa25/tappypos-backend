-- ============================================================
-- V002 — device_tokens: Expo push tokens per user/device
--
-- Tenant table (tenant_id NOT NULL) backing real push notifications
-- (banner when the mobile app is backgrounded). One row per device;
-- expo_push_token is globally unique so re-registration upserts.
-- ============================================================

CREATE TABLE IF NOT EXISTS device_tokens (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    user_id         VARCHAR(50)  NOT NULL,   -- username (matches notifications.user_id)
    expo_push_token VARCHAR(255) NOT NULL,
    platform        VARCHAR(10)  DEFAULT NULL,  -- 'ios' | 'android'
    last_seen_at    TIMESTAMP    DEFAULT NULL,
    legacy_id       VARCHAR(50)  DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    CONSTRAINT uq_device_tokens_token UNIQUE (expo_push_token)
);

ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens FORCE  ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'device_tokens' AND policyname = 'device_tokens_tenant_isolation') THEN
        CREATE POLICY device_tokens_tenant_isolation ON device_tokens
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_device_tokens_user
    ON device_tokens (tenant_id, user_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_device_tokens_legacy
    ON device_tokens (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
