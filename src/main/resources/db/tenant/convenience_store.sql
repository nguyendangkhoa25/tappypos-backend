-- ============================================================
-- TENANT SEED — DEFAULT DATA: CONVENIENCE STORE (TẠP HÓA)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types (18 standard types) ─────────────────────
INSERT INTO product_type (tenant_id, code, name, description) VALUES
    (current_setting('app.current_tenant', true), 'FOOD',         'Thực phẩm',                  'Thực phẩm và đồ ăn'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước giải khát, bia, nước suối'),
    (current_setting('app.current_tenant', true), 'DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Hàng tiêu dùng',             'Hàng tiêu dùng thiết yếu'),
    (current_setting('app.current_tenant', true), 'BIKE',         'Xe đạp / Xe máy',            'Xe đạp và phụ tùng xe máy'),
    (current_setting('app.current_tenant', true), 'HARDWARE',     'Đồ sắt / Dụng cụ',          'Đồ sắt và dụng cụ'),
    (current_setting('app.current_tenant', true), 'CLOTHING',     'Quần áo / May mặc',          'Quần áo và phụ kiện'),
    (current_setting('app.current_tenant', true), 'ELECTRONICS',  'Điện tử',                    'Thiết bị điện tử'),
    (current_setting('app.current_tenant', true), 'FURNITURE',    'Đồ nội thất',                'Nội thất gia đình'),
    (current_setting('app.current_tenant', true), 'BEAUTY',       'Làm đẹp / Chăm sóc cá nhân','Sản phẩm làm đẹp và vệ sinh cá nhân'),
    (current_setting('app.current_tenant', true), 'TOYS',         'Đồ chơi / Trò chơi',        'Đồ chơi và trò chơi'),
    (current_setting('app.current_tenant', true), 'BOOKS',        'Sách / Văn phòng phẩm',     'Sách và văn phòng phẩm'),
    (current_setting('app.current_tenant', true), 'SPORTS',       'Thể thao / Ngoài trời',     'Thiết bị thể thao'),
    (current_setting('app.current_tenant', true), 'AUTO_PARTS',   'Phụ tùng ô tô',             'Phụ tùng và phụ kiện ô tô'),
    (current_setting('app.current_tenant', true), 'APPLIANCES',   'Đồ gia dụng',                'Thiết bị gia dụng'),
    (current_setting('app.current_tenant', true), 'OFFICE',       'Văn phòng phẩm',             'Đồ dùng văn phòng'),
    (current_setting('app.current_tenant', true), 'PET',          'Thú cưng',                  'Thức ăn và phụ kiện thú cưng'),
    (current_setting('app.current_tenant', true), 'HEALTH',       'Sức khỏe / Dinh dưỡng',     'Sản phẩm sức khỏe và dinh dưỡng')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;

-- ── 2. Categories ─────────────────────────────────────────────
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Đồ uống',                     NULL),
    (current_setting('app.current_tenant', true), 'Thực phẩm',                   NULL),
    (current_setting('app.current_tenant', true), 'Bánh kẹo & Snacks',           NULL),
    (current_setting('app.current_tenant', true), 'Vệ sinh cá nhân',             NULL),
    (current_setting('app.current_tenant', true), 'Đồ gia dụng',                 NULL),
    (current_setting('app.current_tenant', true), 'Thuốc lá',                    NULL),
    (current_setting('app.current_tenant', true), 'Sữa & Sản phẩm sữa',         NULL),
    (current_setting('app.current_tenant', true), 'Gia vị & Nước chấm',          NULL);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Nước giải khát',              'Đồ uống'),
    ('Nước suối / Nước tinh khiết', 'Đồ uống'),
    ('Bia & Nước có cồn',           'Đồ uống'),
    ('Trà & Cà phê đóng gói',       'Đồ uống'),
    ('Nước tăng lực',               'Đồ uống'),
    ('Mì gói & Cháo gói',           'Thực phẩm'),
    ('Thực phẩm khô',               'Thực phẩm'),
    ('Bánh quy & Bánh ngọt',        'Bánh kẹo & Snacks'),
    ('Kẹo & Socola',                'Bánh kẹo & Snacks'),
    ('Snack khoai tây',             'Bánh kẹo & Snacks')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp đồ uống & FMCG',         'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp thực phẩm & tiêu dùng',  'VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 8. Loyalty program ───────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 9. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',       2000000,   1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    (current_setting('app.current_tenant', true), 'Vàng',      10000000,  1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    (current_setting('app.current_tenant', true), 'Kim cương', 50000000,  2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND', 4);

-- ── 10. Default print templates ───────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    -- POS receipt: show tax breakdown + cash details (common for retail/FMCG)
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Mặc định', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nHẹn gặp lại!",
  "showAddress": true,
  "showTaxId": false,
  "showOrderNumber": true,
  "showDateTime": true,
  "showCustomer": true,
  "showTaxBreakdown": true,
  "showCashDetails": true,
  "paperWidth": "80mm",
  "autoClose": true
}', TRUE),
    -- Product stamp: barcode + expiry for FMCG
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem sản phẩm', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": true,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE),
    -- Inventory stamp: batch + expiry + location for stock management
    (current_setting('app.current_tenant', true), 'INVENTORY_STAMP', 'Tem kho', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": false,
  "showBarcode": true,
  "showLocation": true,
  "showBatch": true,
  "showExpiry": true,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups & definitions (CONVENIENCE type) ─────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'basic_info', 'Thông tin cơ bản', 1
FROM product_type WHERE code = 'CONVENIENCE' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'storage_handling', 'Bảo quản & Xử lý', 2
FROM product_type WHERE code = 'CONVENIENCE' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Thương hiệu / Nhà sản xuất', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'item_category', 'Danh mục hàng hóa', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'package_size', 'Dung tích / Trọng lượng', 'STRING', FALSE, TRUE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'barcode_upc', 'Mã vạch / Barcode', 'STRING', FALSE, TRUE, FALSE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'expiry_date', 'Hạn sử dụng', 'DATE', FALSE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'storage_requirement', 'Điều kiện bảo quản', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── Shop configuration ────────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
