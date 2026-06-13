-- Cash-drawer reconciliation: one "close" record per business day.
--
-- NOTE: next sequential version after V002. With spring.flyway.validate-on-migrate=false
-- a reused version is silently skipped, so confirm V003 has not already been applied on the
-- target environment (check flyway_schema_history) before deploying.

CREATE TABLE IF NOT EXISTS cash_drawer_close (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    business_date     DATE          NOT NULL,
    opening_amount    NUMERIC(20,0) NOT NULL DEFAULT 0,
    expected_amount   NUMERIC(20,0) NOT NULL DEFAULT 0,
    counted_amount    NUMERIC(20,0) NOT NULL DEFAULT 0,
    difference_amount NUMERIC(20,0) NOT NULL DEFAULT 0,
    note              VARCHAR(500)  DEFAULT NULL,
    closed_by         VARCHAR(100)  DEFAULT NULL,
    closed_at         TIMESTAMP     DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     DEFAULT NULL,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP     DEFAULT NULL,
    -- One reconciliation per shop per day; re-closing updates the same row.
    CONSTRAINT uq_cash_drawer_close_day UNIQUE (tenant_id, business_date)
);

ALTER TABLE cash_drawer_close ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_drawer_close FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON cash_drawer_close
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE INDEX IF NOT EXISTS idx_cash_drawer_close_tenant_date
    ON cash_drawer_close (tenant_id, business_date);
