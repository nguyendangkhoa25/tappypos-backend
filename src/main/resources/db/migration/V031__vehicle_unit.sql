-- ══════════════════════════════════════════════════════════════════════════════
-- V031 — vehicle_unit per-unit registry · VEHICLE_SHOP_SHOP_TYPE_PLAN §4b (headline).
--
-- A vehicle is not a fungible SKU — each chiếc is one titled unit with a unique số khung
-- (frame_no) + số máy (engine_no), its own warranty clock, and a folder of giấy tờ. This
-- table is the SERIAL_NUMBER_MODEL: one row per physical unit, linked to its catalog Product.
-- Gated by the existing PRODUCT feature (no new flag — sub-capability of product).
--
-- New tenant table → RLS policy + legacy_id per backend/CLAUDE.md. Frame/engine numbers are
-- UNIQUE per tenant via partial unique indexes (WHERE ... IS NOT NULL AND deleted = FALSE).
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS vehicle_unit (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    product_id        BIGINT        NOT NULL,             -- catalog Product (the model/listing)
    frame_no          VARCHAR(100)  DEFAULT NULL,         -- số khung (UNIQUE per tenant)
    engine_no         VARCHAR(100)  DEFAULT NULL,         -- số máy   (UNIQUE per tenant)
    license_plate     VARCHAR(20)   DEFAULT NULL,         -- biển số (nếu có)
    color             VARCHAR(50)   DEFAULT NULL,
    odometer_km       INTEGER       DEFAULT NULL,         -- số km đã đi (xe cũ)
    purchase_price    DECIMAL(15,2) DEFAULT NULL,         -- giá nhập / giá thu
    current_value     DECIMAL(15,2) DEFAULT NULL,         -- định giá hiện tại (xe cũ)
    status            VARCHAR(20)   NOT NULL DEFAULT 'IN_STOCK',  -- IN_STOCK/RESERVED/SOLD/TRADED_IN/DAMAGED
    condition_grade   VARCHAR(30)   DEFAULT NULL,         -- Mới / Cũ
    warranty_months   INTEGER       DEFAULT NULL,         -- bảo hành khi bán (tháng)
    warranty_exp      DATE          DEFAULT NULL,         -- ngày hết bảo hành (đặt khi bán)
    paperwork_status  VARCHAR(30)   DEFAULT NULL,         -- Đủ / Thiếu / Đang sang tên
    sold_to           BIGINT        DEFAULT NULL,         -- customer_id người mua
    sold_to_name      VARCHAR(255)  DEFAULT NULL,
    sold_order_id     BIGINT        DEFAULT NULL,
    sold_date         TIMESTAMP     DEFAULT NULL,
    notes             TEXT          DEFAULT NULL,
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    created_by        VARCHAR(255)  DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,
    CONSTRAINT chk_vehicle_unit_status
        CHECK (status IN ('IN_STOCK','RESERVED','SOLD','TRADED_IN','DAMAGED'))
);

CREATE INDEX IF NOT EXISTS idx_vehicle_unit_tenant_status
    ON vehicle_unit (tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vehicle_unit_product
    ON vehicle_unit (tenant_id, product_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vehicle_unit_plate
    ON vehicle_unit (tenant_id, license_plate) WHERE license_plate IS NOT NULL AND deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vehicle_unit_legacy
    ON vehicle_unit (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

-- Frame/engine numbers must be unique within a tenant (only for live, non-null values).
CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_unit_frame_no
    ON vehicle_unit (tenant_id, frame_no) WHERE frame_no IS NOT NULL AND deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_unit_engine_no
    ON vehicle_unit (tenant_id, engine_no) WHERE engine_no IS NOT NULL AND deleted = FALSE;

ALTER TABLE vehicle_unit ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicle_unit FORCE  ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'vehicle_unit' AND policyname = 'vehicle_unit_tenant_isolation') THEN
        CREATE POLICY vehicle_unit_tenant_isolation ON vehicle_unit
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;
