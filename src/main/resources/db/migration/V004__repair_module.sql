-- ══════════════════════════════════════════════════════════════════════════════
-- Device repair / service-ticket module (Sửa chữa / dịch vụ kỹ thuật)
-- Headline feature for the ELECTRONICS vertical. Gated by REPAIR (+ REPAIR_VIEW_ALL
-- sub-feature for own-vs-all), granted only to the ELECTRONICS feature profile.
-- Feature rows inserted BY NAME (idempotent) — never hard-code ids in the
-- 2026010xx range; they collide with the feature seed in V001.
--
-- Extracted out of V001__initial_schema.sql into its own migration so V001 (the
-- applied bootstrap) keeps its original checksum on existing environments.
-- ══════════════════════════════════════════════════════════════════════════════

-- ----- REPAIR + REPAIR_VIEW_ALL feature rows -----
INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('REPAIR', 'Sửa Chữa',
        'Quản lý phiếu sửa chữa thiết bị: tiếp nhận máy, ghi lỗi, báo giá, giao thợ, theo dõi tình trạng và bảo hành sửa chữa',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('REPAIR_VIEW_ALL', 'Xem Tất Cả Phiếu Sửa Chữa',
        'Xem phiếu sửa chữa của tất cả nhân viên; nếu không có quyền này, chỉ xem được phiếu tự tạo',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- ----- Repair tickets (phiếu sửa chữa) -----
CREATE TABLE IF NOT EXISTS repair_tickets (
    id                       BIGSERIAL     PRIMARY KEY,
    tenant_id                VARCHAR(50)   NOT NULL,
    ticket_number            VARCHAR(30)   NOT NULL,
    customer_id              BIGINT,
    customer_name            VARCHAR(255)  NOT NULL,
    customer_phone           VARCHAR(20),
    device_type              VARCHAR(100),
    brand                    VARCHAR(100),
    model                    VARCHAR(100),
    serial_imei              VARCHAR(100),
    reported_fault           TEXT          NOT NULL,
    diagnosis                TEXT,
    quote_amount             DECIMAL(15,2) NOT NULL DEFAULT 0,
    parts_amount             DECIMAL(15,2) NOT NULL DEFAULT 0,
    labor_amount             DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount             DECIMAL(15,2) NOT NULL DEFAULT 0,
    warranty_days            INT           NOT NULL DEFAULT 0,
    assigned_technician_id   BIGINT,
    assigned_technician_name VARCHAR(255),
    status                   VARCHAR(20)   NOT NULL DEFAULT 'RECEIVED', -- RECEIVED | DIAGNOSING | QUOTED | REPAIRING | COMPLETED | DELIVERED | CANCELLED
    is_warranty_claim        BOOLEAN       NOT NULL DEFAULT FALSE,
    note                     TEXT,
    received_at              TIMESTAMP,
    completed_at             TIMESTAMP,
    delivered_at             TIMESTAMP,
    linked_order_id          BIGINT,
    legacy_id                VARCHAR(50)   DEFAULT NULL,
    created_by               VARCHAR(255)  NOT NULL,
    created_at               TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted                  BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMP
);

-- ----- Repair parts (linh kiện thay thế của phiếu) -----
CREATE TABLE IF NOT EXISTS repair_parts (
    id               BIGSERIAL     PRIMARY KEY,
    tenant_id        VARCHAR(50)   NOT NULL,
    repair_ticket_id BIGINT        NOT NULL REFERENCES repair_tickets(id),
    product_id       BIGINT,
    product_name     VARCHAR(255)  NOT NULL,
    quantity         INT           NOT NULL DEFAULT 1,
    unit_price       DECIMAL(15,2) NOT NULL DEFAULT 0,
    line_total       DECIMAL(15,2) NOT NULL DEFAULT 0,
    legacy_id        VARCHAR(50)   DEFAULT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ----- RLS -----
ALTER TABLE repair_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE repair_tickets FORCE  ROW LEVEL SECURITY;
ALTER TABLE repair_parts   ENABLE ROW LEVEL SECURITY;
ALTER TABLE repair_parts   FORCE  ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'repair_tickets' AND policyname = 'repair_tickets_tenant_isolation') THEN
        CREATE POLICY repair_tickets_tenant_isolation ON repair_tickets
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'repair_parts' AND policyname = 'repair_parts_tenant_isolation') THEN
        CREATE POLICY repair_parts_tenant_isolation ON repair_parts
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

-- ----- Indexes -----
CREATE UNIQUE INDEX IF NOT EXISTS idx_repair_tickets_number
    ON repair_tickets (tenant_id, ticket_number) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_repair_tickets_tenant_status
    ON repair_tickets (tenant_id, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_repair_tickets_created_by
    ON repair_tickets (tenant_id, created_by) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_repair_tickets_customer
    ON repair_tickets (tenant_id, customer_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_repair_tickets_legacy
    ON repair_tickets (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_repair_parts_ticket
    ON repair_parts (tenant_id, repair_ticket_id);
CREATE INDEX IF NOT EXISTS idx_repair_parts_legacy
    ON repair_parts (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
