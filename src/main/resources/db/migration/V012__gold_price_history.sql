-- ══════════════════════════════════════════════════════════════════════════════
-- Shop-own gold-price history (jewelry 4b)
-- A snapshot row is written whenever the shop creates/updates a gold-price row, so the
-- gold-price chart can show the shop's OWN buy/sell history (vs the TradingView world feed).
-- Tenant table → RLS + legacy_id per convention.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS gold_price_history (
    id          BIGSERIAL     PRIMARY KEY,
    tenant_id   VARCHAR(50)   NOT NULL,
    code        VARCHAR(50)   NOT NULL,
    label       VARCHAR(100),
    buy         NUMERIC(20,0) NOT NULL DEFAULT 0,
    sell        NUMERIC(20,0) NOT NULL DEFAULT 0,
    pawn        NUMERIC(20,0) NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    legacy_id   VARCHAR(50)   DEFAULT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP
);

ALTER TABLE gold_price_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE gold_price_history FORCE  ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'gold_price_history' AND policyname = 'gold_price_history_tenant_isolation') THEN
        CREATE POLICY gold_price_history_tenant_isolation ON gold_price_history
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_gold_price_history_code_time
    ON gold_price_history (tenant_id, code, recorded_at) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_gold_price_history_legacy
    ON gold_price_history (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
