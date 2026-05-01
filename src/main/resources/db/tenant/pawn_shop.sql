-- ============================================================
-- TENANT SEED — DEFAULT DATA: PAWN SHOP (TIỆM CẦM ĐỒ)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
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

-- ── 2. Categories (pawn shop items) ──────────────────────────
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Điện tử',          NULL),
    (current_setting('app.current_tenant', true), 'Trang sức',         NULL),
    (current_setting('app.current_tenant', true), 'Xe máy / Xe đạp',  NULL),
    (current_setting('app.current_tenant', true), 'Đồ gia dụng',      NULL),
    (current_setting('app.current_tenant', true), 'Thời trang',        NULL),
    (current_setting('app.current_tenant', true), 'Khác',              NULL);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Điện thoại',         'Điện tử'),
    ('Laptop / Máy tính',  'Điện tử'),
    ('Máy tính bảng',      'Điện tử'),
    ('Đồng hồ thông minh', 'Điện tử'),
    ('Máy ảnh',            'Điện tử'),
    ('Thiết bị âm thanh',  'Điện tử'),
    ('Vàng 24K',           'Trang sức'),
    ('Vàng 18K / 14K',     'Trang sức'),
    ('Bạc',                'Trang sức'),
    ('Đồng hồ đeo tay',    'Trang sức'),
    ('Xe máy',             'Xe máy / Xe đạp'),
    ('Xe đạp điện',        'Xe máy / Xe đạp')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Walk-in customer ───────────────────────────────────────
INSERT INTO customers (tenant_id, name, phone, email, notes, deleted)
VALUES (current_setting('app.current_tenant', true), 'Khách lẻ', '0000000000', NULL,
        'Khách hàng lẻ - không có thông tin liên hệ', FALSE)
ON CONFLICT (phone, tenant_id) DO NOTHING;

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
    (current_setting('app.current_tenant', true), 'Đồng',      0,          1.00, '#CD7F32', 'Thành viên cơ bản',         1),
    (current_setting('app.current_tenant', true), 'Bạc',       5000000,    1.25, '#9E9E9E', 'Chi tiêu từ 5 triệu VND',   2),
    (current_setting('app.current_tenant', true), 'Vàng',      20000000,   1.50, '#FFC107', 'Chi tiêu từ 20 triệu VND',  3),
    (current_setting('app.current_tenant', true), 'Kim cương', 100000000,  2.00, '#00BCD4', 'Chi tiêu từ 100 triệu VND', 4);

-- ── 6. Default print template ─────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'RECEIPT', 'Mặc định', '{
  "paperSize": "80mm",
  "showLogo": false,
  "showAddress": true,
  "showPhone": true,
  "showTaxCode": false,
  "showQrCode": false,
  "fontSize": 12,
  "lineSpacing": 1.2,
  "headerLines": ["{{shopName}}", "{{address}}", "ĐT: {{phone}}"],
  "footerLines": ["Cảm ơn quý khách!", "Hẹn gặp lại!"],
  "showOrderNumber": true,
  "showCashier": true,
  "showPaymentMethod": true,
  "showChangeAmount": true,
  "itemColumns": ["name", "qty", "price", "total"]
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 7. Attribute groups & definitions (ELECTRONICS type) ─────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'basic_info', 'Thông tin cơ bản', 1
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'condition', 'Tình trạng', 2
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Hãng sản xuất', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'model', 'Model / Phiên bản', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'serial_number', 'Số serial / IMEI', 'STRING', FALSE, TRUE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'condition_grade', 'Tình trạng', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'condition'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── Shop configuration ────────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations',       '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS',  FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_rate',       '0.0',   'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_type',       '30',    'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_due_date',            '30',    'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_exclude_visible_item','false', 'PAWN', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
