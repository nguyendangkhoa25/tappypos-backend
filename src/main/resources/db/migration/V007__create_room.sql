-- ════════════════════════════════════════════════════════════
-- V007: Lodging domain (hotel / motel / homestay) — ROOM feature
-- Mirrors the BOOKING resource/session lifecycle but for room stays with a
-- multi-night/hourly charge and an in-room item folio. RLS + soft-delete + legacy_id
-- follow the standard tenant-table conventions.
-- ════════════════════════════════════════════════════════════

-- 1. room — a physical room (the board unit)
CREATE TABLE IF NOT EXISTS room (
    id             BIGSERIAL     PRIMARY KEY,
    tenant_id      VARCHAR(100)  NOT NULL,
    room_number    VARCHAR(50)   NOT NULL,
    room_type      VARCHAR(100)  DEFAULT NULL,
    floor          VARCHAR(30)   DEFAULT NULL,
    nightly_rate   DECIMAL(15,2) NOT NULL DEFAULT 0,
    hourly_rate    DECIMAL(15,2) DEFAULT NULL,
    overnight_rate DECIMAL(15,2) DEFAULT NULL,
    max_occupancy  INT           NOT NULL DEFAULT 2,
    status         VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE | OCCUPIED | RESERVED | DIRTY | OOO
    qr_token       VARCHAR(64)   DEFAULT NULL,
    note           TEXT          DEFAULT NULL,
    sort_order     INT           NOT NULL DEFAULT 0,
    legacy_id      VARCHAR(50)   DEFAULT NULL,
    created_at     TIMESTAMP     DEFAULT NOW(),
    updated_at     TIMESTAMP     DEFAULT NOW(),
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP     DEFAULT NULL
);
CREATE INDEX IF NOT EXISTS idx_room_tenant_status ON room (tenant_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_qr_token ON room (qr_token) WHERE qr_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_room_legacy_id ON room (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
ALTER TABLE room ENABLE ROW LEVEL SECURITY;
ALTER TABLE room FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON room
    USING (tenant_id = current_setting('app.current_tenant', true));

-- 2. room_stay — one occupancy session (check-in → check-out)
CREATE TABLE IF NOT EXISTS room_stay (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(100)  NOT NULL,
    stay_number       VARCHAR(20)   NOT NULL,
    room_id           BIGINT        NOT NULL,
    room_number       VARCHAR(50)   NOT NULL,             -- snapshot
    guest_name        VARCHAR(255)  DEFAULT NULL,
    guest_phone       VARCHAR(20)   DEFAULT NULL,
    guest_id_number   VARCHAR(50)   DEFAULT NULL,         -- CCCD / passport
    customer_id       BIGINT        DEFAULT NULL,
    adults            INT           NOT NULL DEFAULT 1,
    billing_mode      VARCHAR(20)   NOT NULL DEFAULT 'NIGHTLY',  -- NIGHTLY | HOURLY | OVERNIGHT
    rate              DECIMAL(15,2) NOT NULL DEFAULT 0,    -- snapshot of unit rate at check-in
    checkin_at        TIMESTAMP     NOT NULL,
    expected_checkout TIMESTAMP     DEFAULT NULL,
    checkout_at       TIMESTAMP     DEFAULT NULL,
    units             INT           NOT NULL DEFAULT 1,    -- nights or hours billed
    room_charge       DECIMAL(15,2) NOT NULL DEFAULT 0,
    deposit           DECIMAL(15,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'IN_HOUSE',  -- IN_HOUSE | CHECKED_OUT | CANCELLED
    linked_order_id   BIGINT        DEFAULT NULL,
    note              TEXT          DEFAULT NULL,
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    created_by        VARCHAR(255)  DEFAULT NULL,
    created_at        TIMESTAMP     DEFAULT NOW(),
    updated_at        TIMESTAMP     DEFAULT NOW(),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP     DEFAULT NULL,
    CONSTRAINT fk_room_stay_room FOREIGN KEY (room_id) REFERENCES room (id)
);
CREATE INDEX IF NOT EXISTS idx_room_stay_tenant_status ON room_stay (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_room_stay_room ON room_stay (room_id);
CREATE INDEX IF NOT EXISTS idx_room_stay_legacy_id ON room_stay (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
ALTER TABLE room_stay ENABLE ROW LEVEL SECURITY;
ALTER TABLE room_stay FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON room_stay
    USING (tenant_id = current_setting('app.current_tenant', true));

-- 3. room_stay_item — in-room item folio line (coke, snack, …)
CREATE TABLE IF NOT EXISTS room_stay_item (
    id           BIGSERIAL     PRIMARY KEY,
    tenant_id    VARCHAR(100)  NOT NULL,
    stay_id      BIGINT        NOT NULL,
    product_id   BIGINT        DEFAULT NULL,
    product_name VARCHAR(255)  NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    unit_price   DECIMAL(15,2) NOT NULL DEFAULT 0,
    source       VARCHAR(20)   NOT NULL DEFAULT 'STAFF',  -- STAFF | QR
    note         VARCHAR(500)  DEFAULT NULL,
    created_by   VARCHAR(255)  DEFAULT NULL,
    created_at   TIMESTAMP     DEFAULT NOW(),
    updated_at   TIMESTAMP     DEFAULT NOW(),
    deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP     DEFAULT NULL,
    CONSTRAINT fk_room_stay_item_stay FOREIGN KEY (stay_id) REFERENCES room_stay (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_room_stay_item_stay ON room_stay_item (stay_id);
ALTER TABLE room_stay_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE room_stay_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON room_stay_item
    USING (tenant_id = current_setting('app.current_tenant', true));

-- 4. Link orders back to a room stay (nullable — no effect on existing orders)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS room_stay_id BIGINT DEFAULT NULL;
