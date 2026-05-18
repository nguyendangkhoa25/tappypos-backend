-- ============================================================
-- TENANT SEED — DEFAULT DATA: PAWN SHOP (TIỆM CẦM ĐỒ)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
-- Pawn shops accept gold/jewelry, electronics, watches, vehicles, and appliances.
INSERT INTO product_type (tenant_id, code, name, description) VALUES
    (current_setting('app.current_tenant', true), 'JEWELRY',     'Vàng bạc đá quý', 'Nữ trang, vàng, bạc, đá quý'),
    (current_setting('app.current_tenant', true), 'ELECTRONICS', 'Điện tử',         'Điện thoại, laptop, máy tính bảng, thiết bị điện tử'),
    (current_setting('app.current_tenant', true), 'WATCH',       'Đồng hồ',         'Đồng hồ đeo tay cao cấp'),
    (current_setting('app.current_tenant', true), 'BIKE',        'Xe máy / Xe điện','Xe máy, xe đạp điện, xe tay ga'),
    (current_setting('app.current_tenant', true), 'APPLIANCES',  'Đồ gia dụng',     'Tivi, tủ lạnh, máy giặt, điều hòa'),
    (current_setting('app.current_tenant', true), 'OTHER',       'Khác',            'Các tài sản cầm đồ khác')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;

-- ── 2. Categories ─────────────────────────────────────────────
-- Top-level categories matching typical pawn shop asset classes.
-- Gold and silver children are also referenced by gold_price.category_id.
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Vàng',        NULL),
    (current_setting('app.current_tenant', true), 'Bạc',         NULL),
    (current_setting('app.current_tenant', true), 'Đá quý',      NULL),
    (current_setting('app.current_tenant', true), 'Đồng hồ',     NULL),
    (current_setting('app.current_tenant', true), 'Điện tử',     NULL),
    (current_setting('app.current_tenant', true), 'Xe máy',      NULL),
    (current_setting('app.current_tenant', true), 'Đồ gia dụng', NULL),
    (current_setting('app.current_tenant', true), 'Khác',        NULL);

-- Gold purity grades (Vàng > children) — also linked to gold_price.category_id
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('9999', 'Vàng'), ('610',  'Vàng'), ('750',  'Vàng'), ('980',  'Vàng'),
    ('990',  'Vàng'), ('680',  'Vàng'), ('950',  'Vàng'), ('23K',  'Vàng'),
    ('17K',  'Vàng'), ('16K',  'Vàng'), ('15K',  'Vàng'), ('600',  'Vàng'), ('10K',  'Vàng')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- Silver purity grades (Bạc > children) — also linked to gold_price.category_id
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('925',  'Bạc'),
    ('950',  'Bạc'),
    ('9999', 'Bạc')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- Gemstone types (Đá quý > children)
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Kim cương', 'Đá quý'),
    ('Hồng ngọc', 'Đá quý'),
    ('Lục bảo',   'Đá quý'),
    ('Sapphire',  'Đá quý'),
    ('Topaz',     'Đá quý'),
    ('Ngọc trai', 'Đá quý'),
    ('Khác',      'Đá quý')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- Watch sub-types (Đồng hồ > children)
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Đồng hồ đeo tay', 'Đồng hồ'),
    ('Đồng hồ bàn',     'Đồng hồ')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- Electronics sub-types (Điện tử > children)
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Điện thoại',          'Điện tử'),
    ('Laptop / Máy tính',   'Điện tử'),
    ('Máy tính bảng',       'Điện tử'),
    ('Đồng hồ thông minh',  'Điện tử'),
    ('Máy ảnh',             'Điện tử'),
    ('Thiết bị âm thanh',   'Điện tử')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- Motorcycle types (Xe máy > children)
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Xe số',     'Xe máy'),
    ('Xe tay ga', 'Xe máy'),
    ('Xe điện',   'Xe máy')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- Appliance types (Đồ gia dụng > children)
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Tivi',         'Đồ gia dụng'),
    ('Tủ lạnh',      'Đồ gia dụng'),
    ('Máy giặt',     'Đồ gia dụng'),
    ('Điều hòa',     'Đồ gia dụng'),
    ('Loa / Amply',  'Đồ gia dụng')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Gold prices (giá vàng theo tuổi) ───────────────────────
-- Prices in VNĐ/chỉ (1 chỉ = 3.75g). Update daily in production.
-- category_id links to the Vàng/Bạc purity categories seeded above.
INSERT INTO gold_price (tenant_id, code, label, pawn, sell, buy, show_in_board, display_order, category_id)
SELECT
    current_setting('app.current_tenant', true),
    gp.code, gp.label,
    gp.pawn::DECIMAL(20,0), gp.sell::DECIMAL(20,0), gp.buy::DECIMAL(20,0),
    gp.show_in_board, gp.display_order,
    c.id
FROM (VALUES
    ('9999',    'Vàng 9999',  8000000, 9040000, 8780000, TRUE,   1, 'Vàng', '9999'),
    ('610',     'Vàng 610',   7300000, 9120000, 8440000, TRUE,   2, 'Vàng', '610'),
    ('980',     'Vàng 980',   6800000, 7660000, 7500000, TRUE,   3, 'Vàng', '980'),
    ('750',     'Vàng 750',   4000000, 4550000, 4150000, TRUE,   4, 'Vàng', '750'),
    ('950',     'Vàng 950',   5000000, 6540000, 6340000, FALSE,  5, 'Vàng', '950'),
    ('990',     'Vàng 990',   6500000, 7240000, 7140000, FALSE,  6, 'Vàng', '990'),
    ('680',     'Vàng 680',   3900000, 4580000, 4200000, FALSE,  7, 'Vàng', '680'),
    ('23K',     'Vàng 23K',   5000000, 6600000, 6420000, FALSE,  8, 'Vàng', '23K'),
    ('17K',     'Vàng 17K',   3400000, 4150000, 3800000, FALSE,  9, 'Vàng', '17K'),
    ('15K',     'Vàng 15K',   3400000, 4150000, 3800000, FALSE, 10, 'Vàng', '15K'),
    ('16K',     'Vàng 16K',   3400000, 4150000, 3800000, FALSE, 11, 'Vàng', '16K'),
    ('600',     'Vàng 600',   4000000, 4500000, 3800000, FALSE, 12, 'Vàng', '600'),
    ('10K',     'Vàng 10K',         0,       0,       0, FALSE, 13, 'Vàng', '10K'),
    ('B925',    'Bạc 925',          0,  130000,   65000, FALSE, 14, 'Bạc',  '925'),
    ('B950',    'Bạc 950',          0,  130000,   65000, FALSE, 15, 'Bạc',  '950'),
    ('Bac9999', 'Bạc 9999',         0,       0,       0, FALSE, 16, 'Bạc',  '9999')
) AS gp(code, label, pawn, sell, buy, show_in_board, display_order, parent_name, cat_name)
LEFT JOIN category p ON p.name = gp.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL
LEFT JOIN category c ON c.name = gp.cat_name
    AND c.parent_id = p.id
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (category_id, tenant_id) WHERE category_id IS NOT NULL
DO UPDATE SET
    sell          = EXCLUDED.sell,
    buy           = EXCLUDED.buy,
    pawn          = EXCLUDED.pawn,
    label         = EXCLUDED.label,
    show_in_board = EXCLUDED.show_in_board;

-- ── 4. Shop configuration ──────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations',        '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS',  FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_rate',        '0.0',   'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_type',        '30',    'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_due_date',             '30',    'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_exclude_visible_item', 'false', 'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_denominations',        '500000,1000000,2000000,5000000,10000000', 'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_accepted_types',       'GOLD,ELECTRONICS,MOTORBIKE,CAR,WATCH,REAL_ESTATE,GENERAL,OTHER', 'PAWN', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 5. Print templates ────────────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    -- Order/sales receipt (80mm thermal)
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Biên nhận bán hàng 80mm', '{
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
}', TRUE),
    -- Pawn stamp: default layout — prints shop name + row labels, for blank paper
    (current_setting('app.current_tenant', true), 'PAWN_STAMP', 'Mặc định', '{"variant":"default"}', TRUE),
    -- Pawn stamp: custom layout — data only, for pre-printed paper with shop name & labels already on it
    (current_setting('app.current_tenant', true), 'PAWN_STAMP', 'Tùy chỉnh', '{"variant":"custom"}', FALSE),
    -- Asset label for pawned / buyback items
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem tài sản cầm đồ', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": false,
  "showLocation": true,
  "showBatch": false,
  "showExpiry": false,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 6. Loyalty program ────────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 7. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,          1.00, '#CD7F32', 'Thành viên cơ bản',          1),
    (current_setting('app.current_tenant', true), 'Bạc',       5000000,    1.25, '#9E9E9E', 'Chi tiêu từ 5 triệu VNĐ',   2),
    (current_setting('app.current_tenant', true), 'Vàng',      20000000,   1.50, '#FFC107', 'Chi tiêu từ 20 triệu VNĐ',  3),
    (current_setting('app.current_tenant', true), 'Kim cương', 100000000,  2.00, '#00BCD4', 'Chi tiêu từ 100 triệu VNĐ', 4);

-- ── 8. Attribute groups & definitions — ELECTRONICS ──────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'device_info', 'Thông tin thiết bị', 1
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'condition_info', 'Tình trạng', 2
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'brand', 'Hãng sản xuất', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'device_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'model', 'Model / Phiên bản', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'device_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'serial_number', 'Số serial / IMEI', 'STRING', FALSE, TRUE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'device_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'condition_grade', 'Tình trạng', 'STRING', TRUE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'condition_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'accessories_included', 'Phụ kiện kèm theo', 'TEXT', FALSE, FALSE, FALSE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'condition_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 9. Attribute groups & definitions — WATCH ────────────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'watch_info', 'Thông tin đồng hồ', 1
FROM product_type WHERE code = 'WATCH' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'brand', 'Hãng sản xuất', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'watch_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'WATCH' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'model', 'Model', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'watch_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'WATCH' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'condition_grade', 'Tình trạng', 'STRING', TRUE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'watch_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'WATCH' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 10. Attribute groups & definitions — BIKE ─────────────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'vehicle_info', 'Thông tin xe', 1
FROM product_type WHERE code = 'BIKE' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'vehicle_condition', 'Tình trạng xe', 2
FROM product_type WHERE code = 'BIKE' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'brand', 'Hãng xe', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'model', 'Model / Dòng xe', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'year_of_manufacture', 'Năm sản xuất', 'NUMBER', TRUE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'engine_cc', 'Dung tích động cơ (cc)', 'NUMBER', FALSE, FALSE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'license_plate', 'Biển số xe', 'STRING', FALSE, TRUE, FALSE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'chassis_number', 'Số khung', 'STRING', FALSE, TRUE, FALSE, 6
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'engine_number', 'Số máy', 'STRING', FALSE, TRUE, FALSE, 7
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'condition_grade', 'Tình trạng xe', 'STRING', TRUE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_condition'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'color', 'Màu sắc', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'vehicle_condition'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11. Attribute groups & definitions — JEWELRY ─────────────
-- For pawned gold/silver items: weight and gem info matter most.

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'weight_info', 'Trọng lượng', 1
FROM product_type WHERE code = 'JEWELRY' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'material_info', 'Thông tin vật liệu', 2
FROM product_type WHERE code = 'JEWELRY' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'total_weight', 'Tổng trọng lượng (chỉ)', 'NUMBER', TRUE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'weight_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gold_weight', 'Trọng lượng vàng (chỉ)', 'NUMBER', TRUE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'weight_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gem_weight', 'Trọng lượng đá (chỉ)', 'NUMBER', FALSE, FALSE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'weight_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gem_type', 'Loại đá quý', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'material_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Gold brand vendors (chành vàng) ──────────────────────
INSERT INTO vendors (tenant_id, code, name, address, notes, payment_terms) VALUES
    (current_setting('app.current_tenant', true), 'TKJ',   'CT TNHHVBTMDV TUẤN KIỆT',           '854-856 Trần Hưng Đạo, P.7, Q.5, TP.HCM',      'TKJ (Tuấn Kiệt)',        'NET_30'),
    (current_setting('app.current_tenant', true), 'KNP',   'CT TNHH VBDQ Kim Ngân Phát',         'ĐT746B, KP4, P.Hội Nghĩa, TU, BD',              'KNP (Kim Ngân Phát)',    'NET_30'),
    (current_setting('app.current_tenant', true), 'KLL',   'DNTN KDV KIM LOAN TUẤN',             '57 Nghĩa Thục, P.5, Q.5, TP.HCM',               'K*L* (Kim Loan Tuấn)',   'NET_30'),
    (current_setting('app.current_tenant', true), 'AHOA',  'CT TNHH VBĐQ HÒA HIẾU',             '151-155 Trần Tuấn Khải, P.5, Q.5, TP.HCM',      'A.HOA (Hòa Hiếu)',      'NET_30'),
    (current_setting('app.current_tenant', true), 'PPJ',   'CT TNHH KDVBDQ Kim Sen',             '5 Chiêu Anh Các, P.5, Q.5, TP.HCM',             'PPJ (Kim Sen)',           'NET_30'),
    (current_setting('app.current_tenant', true), 'VJC',   'CT TNHH MTV VBĐQ Tp HCM',            '3-5 Hồ Tùng Mậu, P.Ng.Thị Bình, Q.1, TP.HCM',  'VJC (Sài Gòn)',         'NET_30'),
    (current_setting('app.current_tenant', true), 'SJC',   'Công ty TNHH MTV VBĐQ Sài Gòn',      '418-420 Nguyễn Thị Minh Khai, Q.3, TP.HCM',    'SJC',                    'NET_30'),
    (current_setting('app.current_tenant', true), 'PNJ',   'Cty CP VBDQ PNJ Phú Nhuận',           '170E Phan Đăng Lưu, Q.Phú Nhuận, TP.HCM',      'PNJ (Phú Nhuận)',       'NET_30'),
    (current_setting('app.current_tenant', true), 'PTJ',   'Bạc Phú Thanh',                       '64 Nghĩa Thục, P.5, Q.5, TP.HCM',               'PTJ (Phú Thanh)',        'NET_30')
ON CONFLICT (code, tenant_id) DO NOTHING;
