-- Contact leads from the public landing page registration form
CREATE TABLE IF NOT EXISTS contact_leads (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    phone       VARCHAR(20)   NOT NULL,
    shop_type   VARCHAR(50),
    note        VARCHAR(1000),
    source      VARCHAR(50)   DEFAULT 'LANDING_PAGE',
    status      VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    admin_note  VARCHAR(1000),
    deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contact_leads_created_at ON contact_leads (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contact_leads_status ON contact_leads (status);

-- New master feature for managing trial registration requests
INSERT INTO features (id, tenant_id, name, display_name, description, active, deleted)
VALUES (202601029, NULL, 'CONTACT_LEAD_MGMT', 'Đăng Ký Dùng Thử', 'Xem và quản lý các yêu cầu đăng ký dùng thử từ trang chủ', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

-- Assign CONTACT_LEAD_MGMT to MASTER_TENANT role
INSERT INTO role_features (tenant_id, role_id, feature_id)
VALUES (NULL, 202600001, 202601029)
ON CONFLICT (role_id, feature_id) DO NOTHING;
