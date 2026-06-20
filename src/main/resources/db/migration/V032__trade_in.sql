-- ══════════════════════════════════════════════════════════════════════════════
-- V032 — trade_in (thu cũ đổi mới / mua xe cũ) · VEHICLE_SHOP_SHOP_TYPE_PLAN §4c.
--
-- The shop values a used vehicle brought in by a seller and either:
--   • NETS it against a new-vehicle sale (giá xe mới − giá thu = phải trả), or
--   • buys it outright STANDALONE (no linked sale) — converges with the Buyback "acquire →
--     list → resell → margin" pattern but vehicle-native (số khung/số máy → resale vehicle_unit).
-- On completion it auto-creates a used vehicle_unit (status TRADED_IN → IN_STOCK) + resale Product.
-- Gated by the new TRADE_IN feature (+ TRADE_IN_VIEW_ALL granular own-vs-all scope).
--
-- New tenant table → RLS policy + legacy_id per backend/CLAUDE.md.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS trade_in (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    trade_in_number   VARCHAR(30)   NOT NULL,            -- mã phiếu (TI-yyyyMMdd-xxx)
    -- seller
    seller_id         BIGINT        DEFAULT NULL,        -- customer_id (null = walk-in)
    seller_name       VARCHAR(255)  DEFAULT NULL,
    seller_phone      VARCHAR(20)   DEFAULT NULL,
    seller_id_number  VARCHAR(30)   DEFAULT NULL,        -- CCCD
    -- incoming used vehicle
    vehicle_type      VARCHAR(30)   DEFAULT NULL,        -- MOTORBIKE / E_BIKE / BICYCLE
    brand             VARCHAR(100)  DEFAULT NULL,
    model             VARCHAR(100)  DEFAULT NULL,
    year              INTEGER       DEFAULT NULL,
    frame_no          VARCHAR(100)  DEFAULT NULL,        -- số khung
    engine_no         VARCHAR(100)  DEFAULT NULL,        -- số máy
    license_plate     VARCHAR(20)   DEFAULT NULL,
    color             VARCHAR(50)   DEFAULT NULL,
    odometer_km       INTEGER       DEFAULT NULL,
    condition_notes   TEXT          DEFAULT NULL,
    trade_value       DECIMAL(15,2) NOT NULL,            -- giá thu xe cũ (valuation)
    -- settlement
    mode              VARCHAR(20)   NOT NULL DEFAULT 'NETTED',  -- NETTED (đổi mới) / STANDALONE (mua đứt)
    new_sale_order_id BIGINT        DEFAULT NULL,        -- linked new-vehicle sale order (NETTED)
    new_price         DECIMAL(15,2) DEFAULT NULL,        -- giá xe mới (NETTED)
    net_amount        DECIMAL(15,2) DEFAULT NULL,        -- new_price − trade_value (phải trả)
    -- resale linkage (auto-created used unit)
    resale_product_id BIGINT        DEFAULT NULL,
    resale_unit_id    BIGINT        DEFAULT NULL,        -- vehicle_unit created from this trade-in
    status            VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED', -- COMPLETED / CANCELLED
    canceled_reason   VARCHAR(255)  DEFAULT NULL,
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    created_by        VARCHAR(255)  NOT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,
    CONSTRAINT chk_trade_in_value  CHECK (trade_value >= 0),
    CONSTRAINT chk_trade_in_mode   CHECK (mode IN ('NETTED','STANDALONE')),
    CONSTRAINT chk_trade_in_status CHECK (status IN ('COMPLETED','CANCELLED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_trade_in_number
    ON trade_in (tenant_id, trade_in_number) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_trade_in_tenant_status
    ON trade_in (tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_trade_in_created_by
    ON trade_in (tenant_id, created_by) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_trade_in_seller
    ON trade_in (tenant_id, seller_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_trade_in_legacy
    ON trade_in (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE trade_in ENABLE ROW LEVEL SECURITY;
ALTER TABLE trade_in FORCE  ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'trade_in' AND policyname = 'trade_in_tenant_isolation') THEN
        CREATE POLICY trade_in_tenant_isolation ON trade_in
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;
