-- ============================================================
-- TENANT SEED — FOOD & BEVERAGE / QUÁN ĐỒ ĂN - ĐỒ UỐNG
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'FOOD',     'Đồ ăn',   'Món ăn, đồ ăn vặt', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE', 'Đồ uống', 'Nước giải khát, cà phê, trà, bia', 'TRACKED', 'cup')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ─────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Đồ ăn',     NULL),
    (current_setting('app.current_tenant', true), 'Đồ uống',   NULL),
    (current_setting('app.current_tenant', true), 'Đồ ăn vặt', NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Cơm & Mì',          'Đồ ăn'),
    ('Món chính',         'Đồ ăn'),
    ('Cà phê',            'Đồ uống'),
    ('Trà & Trà sữa',     'Đồ uống'),
    ('Nước ngọt & Suối',  'Đồ uống'),
    ('Bia & Nước có cồn', 'Đồ uống'),
    ('Bánh & Snack',      'Đồ ăn vặt'),
    ('Trái cây',          'Đồ ăn vặt')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp đồ uống', 'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp thực phẩm', 'VND-002', NULL, NULL, 'NET_15', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

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

-- ── 10. Attribute groups & definitions (BEVERAGE) ─────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'bev_options', 'Tùy chọn phục vụ', 1
FROM product_type WHERE code = 'BEVERAGE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'size_options', 'Kích cỡ (S/M/L)', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_options'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'sweetness_level', 'Độ ngọt', 'STRING', FALSE, FALSE, FALSE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_options'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'ice_level', 'Lượng đá', 'STRING', FALSE, FALSE, FALSE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_options'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute groups & definitions (FOOD)
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'food_info', 'Thông tin món ăn', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'spicy_level', 'Độ cay', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'is_vegan', 'Món chay', 'BOOLEAN', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
