-- ══════════════════════════════════════════════════════════════════════════════
-- V035 — Ký gửi / consignment · BOOK_STORE_SHOP_TYPE_PLAN §4d.
--
-- A publisher / nhà phát hành places stock at the shop (phiếu ký gửi); the shop owes
-- the publisher only for the units that actually sell, and settles by sales over a
-- period. Each consigned title is an ordinary Product, so "units sold" is a passive
-- query over order_items (no checkout coupling). This adds the placement header +
-- line items; settlement is computed on read and stamped onto the header when closed.
--
-- Gated by a dedicated CONSIGNMENT feature (+ CONSIGNMENT_VIEW_ALL sub-feature for the
-- owner-vs-own list scope), granted to the BOOK_STORE profile only.
--
-- Two new tenant tables → RLS policy + legacy_id per backend/CLAUDE.md. Columns follow
-- the BaseEntity/TenantAwareEntity convention (id, tenant_id, created_at, updated_at,
-- deleted, deleted_at).
-- ══════════════════════════════════════════════════════════════════════════════

-- ----- Feature rows (inserted BY NAME, idempotent) -----
INSERT INTO features (name, display_name, description, active, deleted)
VALUES
    ('CONSIGNMENT', 'Ký Gửi Hàng',
     'Nhận hàng ký gửi từ nhà cung cấp/NXB, theo dõi số lượng đã bán và thanh toán theo doanh số; dùng cho nhà sách, cửa hàng ký gửi',
     TRUE, FALSE),
    ('CONSIGNMENT_VIEW_ALL', 'Xem Tất Cả Phiếu Ký Gửi',
     'Xem phiếu ký gửi của tất cả nhân viên; nếu không có quyền này, chỉ xem được phiếu tự tạo',
     TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- ----- Placement header -----
CREATE TABLE IF NOT EXISTS consignment (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    publisher_id      BIGINT        DEFAULT NULL,   -- vendors.id (nhà cung cấp ký gửi); null = ad-hoc
    publisher_name    VARCHAR(255)  NOT NULL,       -- denormalised so the slip prints standalone
    placement_number  VARCHAR(30)   NOT NULL,       -- mã phiếu ký gửi (KG-yyyyMMdd-xxx)
    placement_date    DATE          NOT NULL,       -- ngày nhận hàng ký gửi
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / SETTLED / CANCELLED
    note              VARCHAR(500)  DEFAULT NULL,
    settled_from      DATE          DEFAULT NULL,   -- kỳ thanh toán (từ)
    settled_to        DATE          DEFAULT NULL,   -- kỳ thanh toán (đến)
    settled_date      TIMESTAMP     DEFAULT NULL,
    settled_amount    DECIMAL(15,2) DEFAULT NULL,   -- tổng tiền phải trả NCC theo doanh số
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    created_by        VARCHAR(100)  DEFAULT NULL,
    updated_by        VARCHAR(100)  DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     DEFAULT NOW(),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP     DEFAULT NULL,
    CONSTRAINT chk_consignment_status CHECK (status IN ('ACTIVE','SETTLED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_consignment_tenant    ON consignment (tenant_id);
CREATE INDEX IF NOT EXISTS idx_consignment_status    ON consignment (tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_consignment_publisher ON consignment (tenant_id, publisher_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_consignment_legacy_id ON consignment (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE consignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE consignment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON consignment
    USING (tenant_id = current_setting('app.current_tenant', true));

-- ----- Line items (one consigned title each, linked to its Product) -----
CREATE TABLE IF NOT EXISTS consignment_item (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    consignment_id    BIGINT        NOT NULL,
    product_id        BIGINT        DEFAULT NULL,   -- the consigned title as an ordinary Product
    product_name      VARCHAR(255)  NOT NULL,       -- denormalised
    quantity_placed   INT           NOT NULL DEFAULT 0,   -- số lượng nhận ký gửi
    unit_price        DECIMAL(15,2) NOT NULL DEFAULT 0,    -- giá phải trả NCC mỗi cuốn bán được
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     DEFAULT NOW(),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP     DEFAULT NULL,
    CONSTRAINT chk_consignment_item_qty CHECK (quantity_placed >= 0)
);

CREATE INDEX IF NOT EXISTS idx_consignment_item_tenant     ON consignment_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_consignment_item_parent     ON consignment_item (consignment_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_consignment_item_product    ON consignment_item (tenant_id, product_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_consignment_item_legacy_id  ON consignment_item (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE consignment_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE consignment_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON consignment_item
    USING (tenant_id = current_setting('app.current_tenant', true));
