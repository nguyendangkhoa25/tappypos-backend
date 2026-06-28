-- ════════════════════════════════════════════════════════════
-- V008: BILLING_MGMT feature (master Billing & Revenue cockpit)
--
-- Master-only feature gating the new operator Billing page (cross-tenant payment ledger,
-- refund, record-offline). Revenue summary on the master dashboard stays under MASTER_DASHBOARD.
-- Granted to the MASTER_TENANT role only (NOT to AGENT — billing is operator-only).
-- ════════════════════════════════════════════════════════════

-- Insert by name (uq_features_name); let BIGSERIAL assign the id. The V001 seed leaves the sequence
-- behind MAX(id) (explicit ids like 202601030), so realign it first or the serial default collides.
SELECT setval(pg_get_serial_sequence('features', 'id'), (SELECT MAX(id) FROM features));

INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('BILLING_MGMT', 'Quản Lý Doanh Thu',
        'Theo dõi doanh thu, MRR và lịch sử thanh toán gói dịch vụ toàn nền tảng — chỉ dành cho cơ sở dữ liệu chính',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- Grant to the MASTER_TENANT role (id 202600001, seeded in V001). uq_role_feature guards re-runs.
INSERT INTO role_features (role_id, feature_id)
SELECT 202600001, f.id FROM features f WHERE f.name = 'BILLING_MGMT'
ON CONFLICT ON CONSTRAINT uq_role_feature DO NOTHING;
