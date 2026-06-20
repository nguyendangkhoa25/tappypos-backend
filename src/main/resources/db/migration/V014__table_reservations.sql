-- ══════════════════════════════════════════════════════════════════════════════
-- Table reservations (đặt bàn trước) — FnB §4e (restaurant)
-- A date/time/party-size reservation calendar for dine-in tables, beyond the
-- existing reserved-status toggle on shop_table. One table can hold many future
-- reservations. Gated by the existing TABLE_SERVICE feature (no new flag).
-- Tenant-scoped table → RLS policy + legacy_id per project convention.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS table_reservations (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          VARCHAR(50)   NOT NULL,
    table_id           BIGINT        NOT NULL REFERENCES shop_table(id),
    table_label        VARCHAR(100)  NOT NULL,        -- snapshot of the table number for display
    reserved_at        TIMESTAMP     NOT NULL,        -- date + time the guest is expected
    party_size         INT           NOT NULL DEFAULT 2,
    customer_id        BIGINT,
    customer_name      VARCHAR(255),
    customer_phone     VARCHAR(20),
    status             VARCHAR(20)   NOT NULL DEFAULT 'RESERVED', -- RESERVED | SEATED | CANCELLED | NO_SHOW
    note               TEXT,
    legacy_id          VARCHAR(50)   DEFAULT NULL,
    created_by         VARCHAR(255)  NOT NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP
);

-- ── RLS ──────────────────────────────────────────────────────────────────────
ALTER TABLE table_reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE table_reservations FORCE  ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'table_reservations' AND policyname = 'table_reservations_tenant_isolation') THEN
        CREATE POLICY table_reservations_tenant_isolation ON table_reservations
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

-- ── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_table_reservations_when
    ON table_reservations (tenant_id, reserved_at) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_table_reservations_table
    ON table_reservations (tenant_id, table_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_table_reservations_legacy
    ON table_reservations (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
