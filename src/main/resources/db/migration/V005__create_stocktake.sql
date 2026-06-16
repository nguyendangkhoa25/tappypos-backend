-- ════════════════════════════════════════════════════════════
-- V003: Stocktake / inventory verification ("Kiểm Kho")
-- A session-based physical count: the owner scans products and enters the real
-- counted quantity; on apply, every counted product is reconciled to system stock.
-- Two tenant tables, RLS-scoped like every other tenant table.
-- ════════════════════════════════════════════════════════════

-- 1. Stocktake session — one physical-count run (resumable until applied/cancelled)
CREATE TABLE IF NOT EXISTS stocktake_session (
    id           BIGSERIAL     PRIMARY KEY,
    tenant_id    VARCHAR(100)  NOT NULL,
    name         VARCHAR(255)  DEFAULT NULL,
    status       VARCHAR(30)   NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS | COMPLETED | CANCELLED
    note         TEXT          DEFAULT NULL,
    started_by   VARCHAR(100)  DEFAULT NULL,
    started_at   TIMESTAMP     DEFAULT NULL,
    completed_by VARCHAR(100)  DEFAULT NULL,
    completed_at TIMESTAMP     DEFAULT NULL,
    legacy_id    VARCHAR(50)   DEFAULT NULL,
    created_at   TIMESTAMP     DEFAULT NOW(),
    updated_at   TIMESTAMP     DEFAULT NOW(),
    deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP     DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_stocktake_session_tenant_status
    ON stocktake_session (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_stocktake_session_legacy_id
    ON stocktake_session (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE stocktake_session ENABLE ROW LEVEL SECURITY;
ALTER TABLE stocktake_session FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stocktake_session
    USING (tenant_id = current_setting('app.current_tenant', true));

-- 2. Stocktake count — one product's counted quantity within a session
CREATE TABLE IF NOT EXISTS stocktake_count (
    id           BIGSERIAL     PRIMARY KEY,
    tenant_id    VARCHAR(100)  NOT NULL,
    session_id   BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    inventory_id BIGINT        DEFAULT NULL,
    expected_qty BIGINT        NOT NULL DEFAULT 0,   -- system stock snapshot at first count
    counted_qty  BIGINT        NOT NULL DEFAULT 0,   -- real physical quantity entered
    difference   BIGINT        NOT NULL DEFAULT 0,   -- counted - expected
    counted_by   VARCHAR(100)  DEFAULT NULL,
    counted_at   TIMESTAMP     DEFAULT NULL,
    applied      BOOLEAN       NOT NULL DEFAULT FALSE,
    note         VARCHAR(500)  DEFAULT NULL,
    legacy_id    VARCHAR(50)   DEFAULT NULL,
    created_at   TIMESTAMP     DEFAULT NOW(),
    updated_at   TIMESTAMP     DEFAULT NOW(),
    deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP     DEFAULT NULL,
    CONSTRAINT fk_stocktake_count_session FOREIGN KEY (session_id)
        REFERENCES stocktake_session (id) ON DELETE CASCADE,
    -- one count row per product per session; re-scan upserts
    CONSTRAINT uq_stocktake_count_session_product UNIQUE (session_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_stocktake_count_session   ON stocktake_count (session_id);
CREATE INDEX IF NOT EXISTS idx_stocktake_count_legacy_id ON stocktake_count (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE stocktake_count ENABLE ROW LEVEL SECURITY;
ALTER TABLE stocktake_count FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stocktake_count
    USING (tenant_id = current_setting('app.current_tenant', true));
