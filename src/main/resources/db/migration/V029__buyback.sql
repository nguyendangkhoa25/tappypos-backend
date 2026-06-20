-- ══════════════════════════════════════════════════════════════════════════════
-- V029 — Buyback / second-hand purchase ("mua bán đồ cũ") · PAWN_BUYBACK_SPEC §4–§6 (M1).
--
-- The shop buys a used item outright (no loan/interest/redemption — unlike `pawn`), pays cash,
-- and later resells it. This table ties the seller + acquisition price to a resale Product/Order.
-- Gated by the existing PAWN feature (no new flag, no new shop type — PAWN_BUYBACK_SPEC §7-Q3).
--
-- Gated by a dedicated BUYBACK feature, granted to the JEWELRY profile (and assignable by the
-- master admin to any used-goods shop, e.g. motorbike) — NOT to pawn shops.
--
-- Additive only: one feature row + one new tenant table with RLS + legacy_id per backend/CLAUDE.md.
-- ══════════════════════════════════════════════════════════════════════════════

-- ----- BUYBACK feature row (inserted BY NAME, idempotent) -----
INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('BUYBACK', 'Mua Bán Đồ Cũ',
        'Mua đồ cũ của khách rồi bán lại; dùng cho tiệm vàng, cửa hàng xe máy, đồ cũ',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS buyback (
    buyback_id        BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    customer_id       BIGINT        DEFAULT NULL,   -- seller (null = walk-in); CCCD via customers.id_number
    customer_name     VARCHAR(255)  DEFAULT NULL,   -- denormalised (mirrors pawn.customer_name)
    item_name         VARCHAR(255)  NOT NULL,
    item_description  TEXT          DEFAULT NULL,
    item_category     VARCHAR(50)   DEFAULT NULL,   -- mirrors pawn_category buckets
    acquisition_price DECIMAL(15,2) NOT NULL,       -- cash paid to acquire
    resale_price      DECIMAL(15,2) DEFAULT NULL,   -- final sale price (set when SOLD)
    status            VARCHAR(20)   NOT NULL DEFAULT 'PURCHASED',  -- PURCHASED / LISTED / SOLD / CANCELLED
    product_id        BIGINT        DEFAULT NULL,   -- linked resale Product (set when LISTED)
    order_id          BIGINT        DEFAULT NULL,   -- resale Order (set when SOLD)
    purchase_date     TIMESTAMP     NOT NULL,
    sold_date         TIMESTAMP     DEFAULT NULL,
    canceled_reason   VARCHAR(255)  DEFAULT NULL,
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    visible           BOOLEAN       DEFAULT TRUE,
    created_by        VARCHAR(100)  DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(100)  DEFAULT NULL,
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_buyback_acq_price CHECK (acquisition_price >= 0),
    CONSTRAINT chk_buyback_status    CHECK (status IN ('PURCHASED','LISTED','SOLD','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_buyback_tenant    ON buyback (tenant_id);
CREATE INDEX IF NOT EXISTS idx_buyback_status    ON buyback (tenant_id, status) WHERE visible = TRUE;
CREATE INDEX IF NOT EXISTS idx_buyback_legacy_id ON buyback (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE buyback ENABLE ROW LEVEL SECURITY;
ALTER TABLE buyback FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON buyback
    USING (tenant_id = current_setting('app.current_tenant', true));
