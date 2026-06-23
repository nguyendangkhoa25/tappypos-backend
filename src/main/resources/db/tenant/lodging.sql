-- ============================================================
-- TENANT SEED — LODGING (KHÁCH SẠN / NHÀ NGHỈ / HOMESTAY)
-- Shared by HOTEL, MOTEL, HOMESTAY shop types.
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING / WHERE NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types (room-service / minibar) ────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'BEVERAGE',    'Đồ uống',        'Nước suối, nước ngọt, bia trong phòng', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'FOOD',        'Đồ ăn',          'Mì ly, snack, đồ ăn nhẹ',               'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE', 'Tiện ích phòng', 'Đồ dùng và tiện ích tính phí trong phòng', 'TRACKED', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ─────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Đồ uống',        NULL),
    (current_setting('app.current_tenant', true), 'Đồ ăn nhẹ',      NULL),
    (current_setting('app.current_tenant', true), 'Tiện ích phòng', NULL),
    (current_setting('app.current_tenant', true), 'Dịch vụ',        NULL)
ON CONFLICT (name, tenant_id) DO NOTHING;

-- ── 3. Vendor ─────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp đồ uống & tiện ích', 'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 4. Loyalty program ────────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 5. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,         1.00, '#CD7F32', 'Khách thường',              1),
    (current_setting('app.current_tenant', true), 'Bạc',       5000000,   1.25, '#9E9E9E', 'Chi tiêu từ 5 triệu VND',   2),
    (current_setting('app.current_tenant', true), 'Vàng',      20000000,  1.50, '#FFC107', 'Chi tiêu từ 20 triệu VND',  3),
    (current_setting('app.current_tenant', true), 'Kim cương', 80000000,  2.00, '#00BCD4', 'Khách VIP',                 4);

-- ── 6. Attribute group & definition (CONVENIENCE) ─────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'basic_info', 'Thông tin cơ bản', 1
FROM product_type WHERE code = 'CONVENIENCE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO NOTHING;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Thương hiệu', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name;

-- ── 7. Shop configuration ─────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 8. Sample rooms (board starter) ───────────────────────────
-- Two floors of standard + deluxe rooms; rates filled for nightly / hourly /
-- overnight so the shop works regardless of how the owner bills. The owner can
-- rename, re-rate, add, or remove rooms freely.
INSERT INTO room (tenant_id, room_number, room_type, floor, nightly_rate, hourly_rate, overnight_rate, max_occupancy, status, sort_order)
SELECT current_setting('app.current_tenant', true), v.num, v.rtype, v.floor, v.nightly, v.hourly, v.overnight, v.occ, 'AVAILABLE', v.sort
FROM (VALUES
    ('101', 'Phòng tiêu chuẩn', '1', 300000.00, 70000.00, 250000.00, 2, 1),
    ('102', 'Phòng tiêu chuẩn', '1', 300000.00, 70000.00, 250000.00, 2, 2),
    ('103', 'Phòng đôi',        '1', 350000.00, 80000.00, 280000.00, 3, 3),
    ('104', 'Phòng đôi',        '1', 350000.00, 80000.00, 280000.00, 3, 4),
    ('201', 'Phòng VIP',        '2', 500000.00, 120000.00, 400000.00, 4, 5),
    ('202', 'Phòng VIP',        '2', 500000.00, 120000.00, 400000.00, 4, 6)
) AS v(num, rtype, floor, nightly, hourly, overnight, occ, sort)
WHERE NOT EXISTS (
    SELECT 1 FROM room r
    WHERE r.tenant_id = current_setting('app.current_tenant', true)
      AND r.room_number = v.num
      AND r.deleted = FALSE
);
