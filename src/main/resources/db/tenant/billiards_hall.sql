-- ============================================================
-- TENANT SEED — BILLIARDS HALL / QUÁN BIDA
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- Billiards = table-time via the BOOKING engine + drinks/snacks (POS), PROFILE_RENTAL.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'SERVICE',  'Giờ chơi bàn', 'Tính giờ chơi bida theo bàn', 'NO_INVENTORY', 'hour'),
    (current_setting('app.current_tenant', true), 'BEVERAGE', 'Đồ uống',      'Bia, nước ngọt, nước suối', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'FOOD',     'Đồ ăn vặt',    'Đồ ăn nhẹ, snack', 'TRACKED', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ─────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Giờ chơi bàn', NULL),
    (current_setting('app.current_tenant', true), 'Đồ uống',      NULL),
    (current_setting('app.current_tenant', true), 'Đồ ăn vặt',    NULL),
    (current_setting('app.current_tenant', true), 'Thuốc lá',     NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Bàn thường',        'Giờ chơi bàn'),
    ('Bàn VIP',           'Giờ chơi bàn'),
    ('Bia',               'Đồ uống'),
    ('Nước ngọt & Suối',  'Đồ uống'),
    ('Snack & Hạt',       'Đồ ăn vặt')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà phân phối bia & nước', 'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 4. Sample products (drinks + snacks sold at the table) ─────
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('BIL-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1, 'Bia Tiger lon 330ml',      'Bia lon 330ml',                18000, 13000, 'can',    'BEVERAGE'),
    (2, 'Bia Sài Gòn lon 330ml',    'Bia lon 330ml',                16000, 11500, 'can',    'BEVERAGE'),
    (3, 'Heineken lon 330ml',       'Bia lon 330ml',                22000, 16000, 'can',    'BEVERAGE'),
    (4, 'Nước suối Lavie 500ml',    'Nước suối đóng chai 500ml',     7000,  3500, 'bottle', 'BEVERAGE'),
    (5, 'Coca-Cola lon 330ml',      'Nước ngọt có ga lon 330ml',    10000,  6500, 'can',    'BEVERAGE'),
    (6, 'Sting dâu lon 330ml',      'Nước tăng lực lon 330ml',      12000,  7500, 'can',    'BEVERAGE'),
    (7, 'Snack khoai tây',          'Snack khoai tây vị tự nhiên',  12000,  7500, 'piece',  'FOOD'),
    (8, 'Hạt hướng dương',          'Hạt hướng dương rang',         15000,  9000, 'piece',  'FOOD'),
    (9, 'Đậu phộng rang',           'Lạc rang muối',                12000,  7000, 'piece',  'FOOD'),
    (10,'Mực rim',                  'Mực rim ăn vặt',               25000, 16000, 'piece',  'FOOD')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit, type_code)
JOIN product_type pt
    ON pt.code = seq.type_code
   AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product → category links ───────────────────────────────
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('BIL-DEMO-001', 'Đồ uống'),
    ('BIL-DEMO-002', 'Đồ uống'),
    ('BIL-DEMO-003', 'Đồ uống'),
    ('BIL-DEMO-004', 'Đồ uống'),
    ('BIL-DEMO-005', 'Đồ uống'),
    ('BIL-DEMO-006', 'Đồ uống'),
    ('BIL-DEMO-007', 'Đồ ăn vặt'),
    ('BIL-DEMO-008', 'Đồ ăn vặt'),
    ('BIL-DEMO-009', 'Đồ ăn vặt'),
    ('BIL-DEMO-010', 'Đồ ăn vặt')
) AS m(sku, cat)
JOIN product p ON p.sku = m.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = m.cat AND c.parent_id IS NULL
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, category_id) DO NOTHING;

-- ── 6. Inventory for sample products ──────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 100, 20, 50, p.cost_price, 'Quầy', FALSE
FROM product p
WHERE p.sku LIKE 'BIL-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
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

-- ── 10. Attribute groups & definitions (SERVICE) ──────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'table_info', 'Thông tin bàn', 1
FROM product_type WHERE code = 'SERVICE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'table_type', 'Loại bàn (Thường/VIP)', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'table_info'
WHERE pt.code = 'SERVICE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'hourly_rate', 'Giá theo giờ', 'NUMBER', FALSE, FALSE, FALSE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'table_info'
WHERE pt.code = 'SERVICE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 12. Bookable resources (bàn bida) — timer-based BOOKING engine ──────────────
-- Each table is a BookingResource billed by elapsed time at hourly_rate (floored at
-- minimum_charge). Staff open a table as a WALK_IN; checkout creates a linked POS order.
INSERT INTO booking_resources
    (tenant_id, name, resource_type, hourly_rate, minimum_charge, status, sort_order, created_by)
SELECT current_setting('app.current_tenant', true), v.name, 'TABLE', v.rate, v.min_charge, 'ACTIVE', v.sort, 'system'
FROM (VALUES
    ('Bàn 1',     40000, 20000, 1),
    ('Bàn 2',     40000, 20000, 2),
    ('Bàn 3',     40000, 20000, 3),
    ('Bàn 4',     40000, 20000, 4),
    ('Bàn VIP 1', 60000, 30000, 5),
    ('Bàn VIP 2', 60000, 30000, 6)
) AS v(name, rate, min_charge, sort)
WHERE NOT EXISTS (
    SELECT 1 FROM booking_resources br
    WHERE br.tenant_id = current_setting('app.current_tenant', true) AND br.name = v.name
);

-- ── 13. Rate windows (giá giờ vàng) — peak evening rate ─────────────────────────
INSERT INTO booking_resource_rate
    (tenant_id, resource_id, day_kind, start_time, end_time, rate, sort_order, created_by)
SELECT current_setting('app.current_tenant', true), br.id, 'ALL', '17:00'::time, '23:00'::time,
       CASE WHEN br.name LIKE 'Bàn VIP%' THEN 80000 ELSE 55000 END, 1, 'system'
FROM booking_resources br
WHERE br.tenant_id = current_setting('app.current_tenant', true)
  AND br.resource_type = 'TABLE'
  AND NOT EXISTS (
      SELECT 1 FROM booking_resource_rate rr
      WHERE rr.tenant_id = current_setting('app.current_tenant', true)
        AND rr.resource_id = br.id AND rr.start_time = '17:00'::time
  );
