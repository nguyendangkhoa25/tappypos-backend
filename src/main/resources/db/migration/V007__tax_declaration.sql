-- ════════════════════════════════════════════════════════════
-- V007: Khai báo thuế (TAX_DECLARATION)
--
-- Hỗ trợ hộ kinh doanh tự kê khai thuế GTGT + TNCN theo phương pháp tỷ lệ
-- trên doanh thu (luật mới từ 01/01/2026, bỏ thuế khoán).
--
-- Gồm:
--   1) shop_info: thêm business_type + tax_industry_groups
--   2) tax_rate_catalog — bảng MASTER (tenant-agnostic, NO RLS): tỷ lệ % + ngưỡng
--      theo nhóm ngành. Master admin sửa khi luật đổi → KHÔNG hardcode trong code.
--   3) tax_declaration + tax_declaration_line — bảng TENANT (RLS), một tờ khai/kỳ.
--      Tỷ lệ được SNAPSHOT vào line khi tạo nên tờ khai cũ không đổi khi catalog cập nhật.
--   4) feature TAX_DECLARATION
-- ════════════════════════════════════════════════════════════

-- ── 1) Loại hình KD + nhóm ngành thuế mặc định trên shop_info ──────────────
ALTER TABLE shop_info ADD COLUMN IF NOT EXISTS business_type       VARCHAR(20)  DEFAULT NULL;
ALTER TABLE shop_info ADD COLUMN IF NOT EXISTS tax_industry_groups VARCHAR(255) DEFAULT NULL; -- CSV mã nhóm ngành (catalog.code)

-- ── 2) Catalog tỷ lệ thuế (MASTER, không RLS) ──────────────────────────────
CREATE TABLE IF NOT EXISTS tax_rate_catalog (
    id                    BIGSERIAL     PRIMARY KEY,
    code                  VARCHAR(50)   NOT NULL,            -- DISTRIBUTION, ...
    name                  VARCHAR(255)  NOT NULL,            -- "Phân phối, cung cấp hàng hóa"
    vat_rate              DECIMAL(5,2)  NOT NULL,            -- 1.00 = 1%
    pit_rate              DECIMAL(5,2)  NOT NULL,            -- 0.50 = 0.5%
    exempt_threshold_year DECIMAL(15,2) DEFAULT NULL,        -- ngưỡng miễn thuế/năm (cấu hình)
    form_threshold        DECIMAL(15,2) DEFAULT NULL,        -- ngưỡng chuyển mẫu 01/CNKD
    effective_from        DATE          NOT NULL,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order         INT           NOT NULL DEFAULT 0,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP     DEFAULT NULL,
    CONSTRAINT uq_tax_rate_catalog_code UNIQUE (code)
);

-- Seed 4 nhóm ngành theo TT 40/2021 (tỷ lệ có thể chỉnh sau khi luật cập nhật).
-- Ngưỡng để NULL: logic tính dùng giá trị cấu hình; chủ shop/master admin nhập ngưỡng
-- hiện hành khi đã có văn bản chính thức (tránh hardcode số chưa chắc chắn).
INSERT INTO tax_rate_catalog (code, name, vat_rate, pit_rate, effective_from, display_order) VALUES
 ('DISTRIBUTION',                'Phân phối, cung cấp hàng hóa',                     1.00, 0.50, '2026-01-01', 1),
 ('MANUFACTURING_SERVICE_GOODS', 'Sản xuất, vận tải, dịch vụ có gắn hàng hóa',       3.00, 1.50, '2026-01-01', 2),
 ('SERVICE_NO_MATERIAL',         'Dịch vụ, xây dựng không bao thầu nguyên vật liệu', 5.00, 2.00, '2026-01-01', 3),
 ('ASSET_LEASING',               'Cho thuê tài sản',                                 5.00, 5.00, '2026-01-01', 4)
ON CONFLICT (code) DO NOTHING;

-- ── 3) Tờ khai (TENANT, RLS) ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tax_declaration (
    id               BIGSERIAL      PRIMARY KEY,
    tenant_id        VARCHAR(36)    NOT NULL,
    period_type      VARCHAR(10)    NOT NULL DEFAULT 'QUARTER',
    period_year      INT            NOT NULL,
    period_number    INT            NOT NULL,                 -- quý 1..4 (hoặc tháng/0 cho năm)
    business_type    VARCHAR(20)    NOT NULL,
    form_type        VARCHAR(20)    NOT NULL DEFAULT 'FORM_01_CNKD',
    declared_revenue DECIMAL(15,2)  NOT NULL DEFAULT 0,       -- tổng doanh thu chốt
    auto_revenue     DECIMAL(15,2)  NOT NULL DEFAULT 0,       -- doanh thu auto từ POS (đối chiếu)
    total_vat        DECIMAL(15,2)  NOT NULL DEFAULT 0,
    total_pit        DECIMAL(15,2)  NOT NULL DEFAULT 0,
    total_tax        DECIMAL(15,2)  NOT NULL DEFAULT 0,
    status           VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    gov_ref_number   VARCHAR(100)   DEFAULT NULL,             -- mã tham chiếu sau khi nộp (nhập tay)
    submitted_at     TIMESTAMP      DEFAULT NULL,
    notes            TEXT           DEFAULT NULL,
    legacy_id        VARCHAR(50)    DEFAULT NULL,
    created_by       VARCHAR(100)   DEFAULT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP      DEFAULT NULL,
    CONSTRAINT chk_tax_decl_year   CHECK (period_year BETWEEN 2000 AND 2100),
    CONSTRAINT chk_tax_decl_status CHECK (status IN ('DRAFT','FINALIZED','SUBMITTED','CANCELLED')),
    CONSTRAINT uq_tax_decl_period  UNIQUE (tenant_id, period_type, period_year, period_number)
);

CREATE INDEX IF NOT EXISTS idx_tax_declaration_period    ON tax_declaration (tenant_id, period_year DESC, period_number DESC);
CREATE INDEX IF NOT EXISTS idx_tax_declaration_status    ON tax_declaration (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tax_declaration_legacy_id ON tax_declaration (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE tax_declaration ENABLE ROW LEVEL SECURITY;
ALTER TABLE tax_declaration FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tax_declaration
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE TABLE IF NOT EXISTS tax_declaration_line (
    id             BIGSERIAL      PRIMARY KEY,
    tenant_id      VARCHAR(36)    NOT NULL,
    declaration_id BIGINT         NOT NULL,
    industry_code  VARCHAR(50)    NOT NULL,                  -- snapshot từ catalog
    industry_name  VARCHAR(255)   NOT NULL,                  -- snapshot
    revenue        DECIMAL(15,2)  NOT NULL DEFAULT 0,
    vat_rate       DECIMAL(5,2)   NOT NULL,                  -- snapshot
    pit_rate       DECIMAL(5,2)   NOT NULL,                  -- snapshot
    vat_amount     DECIMAL(15,2)  NOT NULL DEFAULT 0,
    pit_amount     DECIMAL(15,2)  NOT NULL DEFAULT 0,
    legacy_id      VARCHAR(50)    DEFAULT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP      DEFAULT NULL,
    CONSTRAINT fk_tax_decl_line_decl FOREIGN KEY (declaration_id) REFERENCES tax_declaration (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tax_decl_line_decl      ON tax_declaration_line (tenant_id, declaration_id);
CREATE INDEX IF NOT EXISTS idx_tax_decl_line_legacy_id ON tax_declaration_line (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE tax_declaration_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE tax_declaration_line FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tax_declaration_line
    USING (tenant_id = current_setting('app.current_tenant', true));

-- ── 4) Feature TAX_DECLARATION ─────────────────────────────────────────────
-- Insert by name (uq_features_name); let BIGSERIAL assign the id to avoid colliding
-- with explicit ids already seeded (e.g. COMMISSION_VIEW_ALL = 202601040). The seed
-- migration leaves the sequence behind MAX(id), so realign it first or the serial
-- default would collide on a fresh install.
SELECT setval(pg_get_serial_sequence('features', 'id'), (SELECT MAX(id) FROM features));

INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('TAX_DECLARATION', 'Khai Báo Thuế',
        'Tính và xuất tờ khai thuế GTGT, TNCN cho hộ kinh doanh theo phương pháp kê khai', TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;
