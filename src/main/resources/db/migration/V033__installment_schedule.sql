-- ══════════════════════════════════════════════════════════════════════════════
-- V033 — installment (trả góp) schedule on CUSTOMER_DEBT · VEHICLE_SHOP_SHOP_TYPE_PLAN §4e.
--
-- Vehicles are routinely sold in N kỳ. A CustomerDebt already records the total owed; this
-- migration extends it with installment metadata + a per-kỳ schedule child table. In-house
-- interest-FREE schedules only (the optional interest_pct column is kept nullable for a future
-- lãi-suất variant but is unused now). Gated by the new INSTALLMENT feature (depends on
-- CUSTOMER_DEBT) + INSTALLMENT_VIEW_ALL granular own-vs-all scope.
--
-- New tenant table → RLS policy + legacy_id per backend/CLAUDE.md.
-- ══════════════════════════════════════════════════════════════════════════════

-- Extend CustomerDebt: an installment contract is a debt with installment_count NOT NULL.
ALTER TABLE customer_debt ADD COLUMN IF NOT EXISTS installment_count INTEGER       DEFAULT NULL;
ALTER TABLE customer_debt ADD COLUMN IF NOT EXISTS down_payment      DECIMAL(15,2)  DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_customer_debt_installment
    ON customer_debt (tenant_id, installment_count) WHERE installment_count IS NOT NULL AND deleted = FALSE;

CREATE TABLE IF NOT EXISTS installment_schedule (
    id              BIGSERIAL     PRIMARY KEY,
    tenant_id       VARCHAR(50)   NOT NULL,
    debt_id         BIGINT        NOT NULL,             -- FK customer_debt.id (the contract)
    order_id        BIGINT        DEFAULT NULL,
    installment_no  INTEGER       NOT NULL,             -- kỳ thứ (1..N)
    due_date        DATE          NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,             -- tiền phải trả kỳ này
    interest_pct    DECIMAL(6,3)  DEFAULT NULL,         -- reserved (interest-free now)
    paid            BOOLEAN       NOT NULL DEFAULT FALSE,
    paid_amount     DECIMAL(15,2) DEFAULT NULL,
    paid_date       DATE          DEFAULT NULL,
    paid_by         VARCHAR(255)  DEFAULT NULL,
    legacy_id       VARCHAR(50)   DEFAULT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    CONSTRAINT chk_installment_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_installment_debt
    ON installment_schedule (tenant_id, debt_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_installment_due
    ON installment_schedule (tenant_id, due_date) WHERE paid = FALSE AND deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_installment_legacy
    ON installment_schedule (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE installment_schedule ENABLE ROW LEVEL SECURITY;
ALTER TABLE installment_schedule FORCE  ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'installment_schedule' AND policyname = 'installment_schedule_tenant_isolation') THEN
        CREATE POLICY installment_schedule_tenant_isolation ON installment_schedule
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;
