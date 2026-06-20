-- ══════════════════════════════════════════════════════════════════════════════
-- Customer debt / công nợ (bán chịu, ghi sổ nợ, thu nợ)
-- Headline gap for the BUILDING_MATERIALS (VLXD) vertical — shops sell on credit to
-- contractors and settle later. Gated by CUSTOMER_DEBT, granted to the RETAIL profile.
-- Feature row inserted BY NAME (idempotent) — never hard-code feature ids.
--
-- Model: one customer_debt row per credit sale (linked to an order, or manual);
-- per-customer balance = SUM(outstanding_amount) of non-deleted, non-PAID debts.
-- debt_payment rows record repayments (thu nợ), each applied to one debt.
-- ══════════════════════════════════════════════════════════════════════════════

-- ----- CUSTOMER_DEBT feature row -----
INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('CUSTOMER_DEBT', 'Công Nợ Khách Hàng',
        'Bán chịu, ghi sổ nợ và thu nợ khách hàng (thường dùng cho cửa hàng vật liệu xây dựng)',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- ----- Customer debts (sổ nợ — một dòng cho mỗi lần bán chịu) -----
CREATE TABLE IF NOT EXISTS customer_debt (
    id                  BIGSERIAL     PRIMARY KEY,
    tenant_id           VARCHAR(50)   NOT NULL,
    customer_id         BIGINT        NOT NULL,
    customer_name       VARCHAR(255),
    order_id            BIGINT,
    order_number        VARCHAR(50),
    original_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
    paid_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    outstanding_amount  DECIMAL(15,2) NOT NULL DEFAULT 0,
    due_date            DATE,
    status              VARCHAR(20)   NOT NULL DEFAULT 'OPEN', -- OPEN | PARTIAL | PAID
    note                TEXT,
    legacy_id           VARCHAR(50)   DEFAULT NULL,
    created_by          VARCHAR(255)  NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP
);

-- ----- Debt payments (thu nợ — mỗi dòng là một lần trả nợ) -----
CREATE TABLE IF NOT EXISTS debt_payment (
    id              BIGSERIAL     PRIMARY KEY,
    tenant_id       VARCHAR(50)   NOT NULL,
    customer_id     BIGINT        NOT NULL,
    debt_id         BIGINT,       -- tenant-scoped in app + RLS; no cross-tenant FK (cf. order_id on customer_debt)
    amount          DECIMAL(15,2) NOT NULL DEFAULT 0,
    method          VARCHAR(30)   NOT NULL DEFAULT 'CASH', -- CASH | TRANSFER | CARD
    note            TEXT,
    paid_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    legacy_id       VARCHAR(50)   DEFAULT NULL,
    created_by      VARCHAR(255)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);

-- ----- RLS -----
ALTER TABLE customer_debt ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_debt FORCE  ROW LEVEL SECURITY;
ALTER TABLE debt_payment  ENABLE ROW LEVEL SECURITY;
ALTER TABLE debt_payment  FORCE  ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'customer_debt' AND policyname = 'customer_debt_tenant_isolation') THEN
        CREATE POLICY customer_debt_tenant_isolation ON customer_debt
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'debt_payment' AND policyname = 'debt_payment_tenant_isolation') THEN
        CREATE POLICY debt_payment_tenant_isolation ON debt_payment
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

-- ----- Indexes -----
CREATE INDEX IF NOT EXISTS idx_customer_debt_customer
    ON customer_debt (tenant_id, customer_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_customer_debt_status
    ON customer_debt (tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_customer_debt_order
    ON customer_debt (tenant_id, order_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_customer_debt_legacy
    ON customer_debt (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_debt_payment_customer
    ON debt_payment (tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_debt_payment_debt
    ON debt_payment (tenant_id, debt_id);
CREATE INDEX IF NOT EXISTS idx_debt_payment_legacy
    ON debt_payment (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
