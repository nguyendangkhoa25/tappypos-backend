-- ============================================================
-- TENANT SEED — BILLIARDS HALL / QUÁN BIDA
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- Billiards = table-time SERVICE + drinks/snacks (POS), mapped to PROFILE_FNB.
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
