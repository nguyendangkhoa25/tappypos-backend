-- ============================================================
-- TENANT SEED — SPORT COURT / SÂN TENNIS · SÂN THỂ THAO
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING/UPDATE or NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
-- Sân thể thao = court-time via the BOOKING engine + drinks/equipment (POS), PROFILE_RENTAL.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'BEVERAGE', 'Đồ uống',     'Nước suối, nước ngọt, nước tăng lực', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'FOOD',     'Đồ ăn nhẹ',   'Snack, bánh, trái cây', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'SERVICE',  'Thuê dụng cụ', 'Thuê vợt, bóng, đèn sân', 'NO_INVENTORY', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ─────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Đồ uống',      NULL),
    (current_setting('app.current_tenant', true), 'Đồ ăn nhẹ',    NULL),
    (current_setting('app.current_tenant', true), 'Thuê dụng cụ', NULL);

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà phân phối nước & đồ thể thao', 'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 4. Sample products (drinks + equipment rental) ────────────
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('SPC-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1, 'Nước suối Lavie 500ml',     'Nước suối đóng chai 500ml',        7000,   3500,  'bottle', 'BEVERAGE'),
    (2, 'Nước tăng lực Sting 330ml', 'Nước tăng lực lon 330ml',         12000,   7500,  'can',    'BEVERAGE'),
    (3, 'Pocari Sweat 500ml',        'Nước bù khoáng chai 500ml',       15000,  10000,  'bottle', 'BEVERAGE'),
    (4, 'Revive chanh muối 500ml',   'Nước bù khoáng chanh muối',       12000,   7500,  'bottle', 'BEVERAGE'),
    (5, 'Khăn lạnh',                 'Khăn lạnh dùng một lần',           5000,   2500,  'piece',  'FOOD'),
    (6, 'Chuối / Trái cây',          'Trái cây tươi theo phần',         15000,   9000,  'piece',  'FOOD'),
    (7, 'Thuê vợt tennis',           'Thuê vợt tennis theo buổi',       50000,      0,  'piece',  'SERVICE'),
    (8, 'Thuê vợt cầu lông',         'Thuê vợt cầu lông theo buổi',     30000,      0,  'piece',  'SERVICE'),
    (9, 'Ống bóng tennis',           'Bán/thuê ống bóng tennis',        80000,  55000,  'piece',  'SERVICE'),
    (10,'Phí bật đèn sân',           'Phụ phí chiếu sáng sân buổi tối', 50000,      0,  'piece',  'SERVICE')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit, type_code)
JOIN product_type pt
    ON pt.code = seq.type_code
   AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product → category links ───────────────────────────────
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('SPC-DEMO-001', 'Đồ uống'),
    ('SPC-DEMO-002', 'Đồ uống'),
    ('SPC-DEMO-003', 'Đồ uống'),
    ('SPC-DEMO-004', 'Đồ uống'),
    ('SPC-DEMO-005', 'Đồ ăn nhẹ'),
    ('SPC-DEMO-006', 'Đồ ăn nhẹ'),
    ('SPC-DEMO-007', 'Thuê dụng cụ'),
    ('SPC-DEMO-008', 'Thuê dụng cụ'),
    ('SPC-DEMO-009', 'Thuê dụng cụ'),
    ('SPC-DEMO-010', 'Thuê dụng cụ')
) AS m(sku, cat)
JOIN product p ON p.sku = m.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = m.cat AND c.parent_id IS NULL
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, category_id) DO NOTHING;

-- ── 6. Inventory (tracked products only — drinks/snacks) ──────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 100, 20, 50, p.cost_price, 'Quầy', FALSE
FROM product p
JOIN product_type pt ON pt.id = p.product_type_id
WHERE p.sku LIKE 'SPC-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
  AND pt.default_inventory_mode = 'TRACKED'
ON CONFLICT (product_id) WHERE variant_id IS NULL AND deleted = false DO NOTHING;

-- ── 7. Loyalty program ───────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 8. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Thành viên', 0,        1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',        500000,   1.25, '#9E9E9E', 'Chi tiêu từ 500K VND',     2),
    (current_setting('app.current_tenant', true), 'Vàng',       2000000,  1.50, '#FFC107', 'Chi tiêu từ 2 triệu VND',  3),
    (current_setting('app.current_tenant', true), 'VIP',        10000000, 2.00, '#00BCD4', 'Chi tiêu từ 10 triệu VND', 4);

-- ── 9. Print templates ────────────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Mặc định', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nHẹn gặp lại!",
  "showAddress": true,
  "showTaxId": false,
  "showOrderNumber": true,
  "showDateTime": true,
  "showCustomer": true,
  "showTaxBreakdown": false,
  "showCashDetails": true,
  "paperWidth": "80mm",
  "autoClose": true
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 10. Attribute groups & definitions (SERVICE / thuê dụng cụ) ─
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'rental_info', 'Thông tin thuê', 1
FROM product_type WHERE code = 'SERVICE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'rental_unit', 'Đơn vị thuê (buổi/giờ)', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'rental_info'
WHERE pt.code = 'SERVICE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 12. Bookable resources (sân) — timer + reservation BOOKING engine ───────────
-- Each court is a BookingResource billed by time at hourly_rate, floored at minimum_charge.
-- Owners reserve a court ahead (RESERVATION) or open a WALK_IN; checkout creates a linked order.
INSERT INTO booking_resources
    (tenant_id, name, resource_type, hourly_rate, minimum_charge, status, sort_order, created_by)
SELECT current_setting('app.current_tenant', true), v.name, 'COURT', v.rate, v.min_charge, 'ACTIVE', v.sort, 'system'
FROM (VALUES
    ('Sân 1', 120000, 60000, 1),
    ('Sân 2', 120000, 60000, 2),
    ('Sân 3', 120000, 60000, 3),
    ('Sân 4', 150000, 75000, 4)
) AS v(name, rate, min_charge, sort)
WHERE NOT EXISTS (
    SELECT 1 FROM booking_resources br
    WHERE br.tenant_id = current_setting('app.current_tenant', true) AND br.name = v.name
);

-- ── 13. Rate windows (giá giờ vàng) — peak evening rate 17:00–22:00 ──────────────
INSERT INTO booking_resource_rate
    (tenant_id, resource_id, day_kind, start_time, end_time, rate, sort_order, created_by)
SELECT current_setting('app.current_tenant', true), br.id, 'ALL', '17:00'::time, '22:00'::time,
       CASE WHEN br.name = 'Sân 4' THEN 200000 ELSE 170000 END, 1, 'system'
FROM booking_resources br
WHERE br.tenant_id = current_setting('app.current_tenant', true)
  AND br.resource_type = 'COURT'
  AND NOT EXISTS (
      SELECT 1 FROM booking_resource_rate rr
      WHERE rr.tenant_id = current_setting('app.current_tenant', true)
        AND rr.resource_id = br.id AND rr.start_time = '17:00'::time
  );
