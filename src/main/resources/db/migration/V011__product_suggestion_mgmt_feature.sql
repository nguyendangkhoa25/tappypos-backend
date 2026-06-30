-- ════════════════════════════════════════════════════════════
-- V011: PRODUCT_SUGGESTION_MGMT feature (master-only)
--
-- Gates the new master-admin "Sản phẩm gợi ý" page that manages the `product_suggestions`
-- table (the default/suggested products offered per shop type at onboarding).
-- Granted to the MASTER_TENANT role only (NOT to AGENT) — same scope as PRODUCT_CATALOG.
-- ════════════════════════════════════════════════════════════

-- Realign the serial first: the V001 seed leaves explicit ids ahead of the sequence,
-- so let BIGSERIAL assign the next id safely (same approach as V008).
SELECT setval(pg_get_serial_sequence('features', 'id'), (SELECT MAX(id) FROM features));

INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('PRODUCT_SUGGESTION_MGMT', 'Quản Lý Sản Phẩm Gợi Ý',
        'Quản lý danh sách sản phẩm gợi ý theo loại cửa hàng dùng khi tạo cửa hàng mới — chỉ dành cho cơ sở dữ liệu chính',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- Grant to the MASTER_TENANT role (id 202600001, seeded in V001). uq_role_feature guards re-runs.
INSERT INTO role_features (role_id, feature_id)
SELECT 202600001, f.id FROM features f WHERE f.name = 'PRODUCT_SUGGESTION_MGMT'
ON CONFLICT ON CONSTRAINT uq_role_feature DO NOTHING;
