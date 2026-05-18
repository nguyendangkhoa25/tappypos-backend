-- ============================================================
-- TENANT SEED — DEFAULT DATA: JEWELRY SHOP (TIỆM VÀNG / TRANG SỨC)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
-- Jewelry shops primarily deal in fine jewelry, watches, and pawned electronics.
INSERT INTO product_type (tenant_id, code, name, description) VALUES
    (current_setting('app.current_tenant', true), 'JEWELRY',     'Vàng bạc đá quý', 'Nữ trang, vàng, bạc, đá quý'),
    (current_setting('app.current_tenant', true), 'WATCH',       'Đồng hồ',         'Đồng hồ đeo tay và đồng hồ bàn'),
    (current_setting('app.current_tenant', true), 'ELECTRONICS', 'Điện tử',         'Điện thoại, máy tính, thiết bị điện tử'),
    (current_setting('app.current_tenant', true), 'OTHER',       'Khác',            'Các mặt hàng khác')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;

-- ── 2. Categories ─────────────────────────────────────────────
-- Gold and silver top-level categories only.
-- Children are also referenced by gold_price.category_id.
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Vàng', NULL),
    (current_setting('app.current_tenant', true), 'Bạc',  NULL);

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

-- ── 3. Jewelry display counters (quầy trưng bày) ──────────────
INSERT INTO jewelry_counters (tenant_id, code, name, sort_order) VALUES
    (current_setting('app.current_tenant', true), 'Unknown',    'Chọn quầy',         0),
    (current_setting('app.current_tenant', true), 'BONG',       'Quầy bông',         1),
    (current_setting('app.current_tenant', true), 'BONGTREO',   'Quầy bông treo',    2),
    (current_setting('app.current_tenant', true), 'NHAN NU',    'Quầy nhẫn nữ',      3),
    (current_setting('app.current_tenant', true), 'NHAN NAM',   'Quầy nhẫn nam',     4),
    (current_setting('app.current_tenant', true), 'NHAN CUOI',  'Quầy nhẫn cặp',     5),
    (current_setting('app.current_tenant', true), 'LAC',        'Quầy lắc',          6),
    (current_setting('app.current_tenant', true), 'XIMEN',      'Quầy vòng ximen',   7),
    (current_setting('app.current_tenant', true), 'VONG EM',    'Quầy vòng em',      8),
    (current_setting('app.current_tenant', true), 'DAY CHUYEN', 'Quầy dây chuyền',   9),
    (current_setting('app.current_tenant', true), 'MAT KIEU',   'Quầy mặt kiểu',    10),
    (current_setting('app.current_tenant', true), 'NHAN TRON',  'Quầy nhẫn trơn',   11)
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 4. Gold prices (giá vàng theo tuổi) ───────────────────────
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

-- ── 5. Shop configuration ──────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'PAWN_INTEREST_VALUE',       '3',       'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'PAWN_EXPIRATION_DAYS',      '45',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'PAWN_INTEREST_CALCULATION', '30',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'PRICE_BOARD_CODE',          '6868689', 'DISPLAY', FALSE),
    (current_setting('app.current_tenant', true), 'JEWELRY_STAMP_TYPE',        '16x30',   'PRINT',   FALSE),
    (current_setting('app.current_tenant', true), 'EXCLUDE_VISIBLE_ITEM',      'NO',      'DISPLAY', FALSE),
    (current_setting('app.current_tenant', true), 'cash_denominations',        '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_rate',        '0.0',     'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_type',        '30',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_due_date',             '30',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_exclude_visible_item', 'false',   'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_denominations',        '500000,1000000,2000000,5000000,10000000', 'PAWN', FALSE),
    (current_setting('app.current_tenant', true), 'POS_MODE',                  'GOLD_TRADING', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 6. Print templates ────────────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    -- Order/sales receipt (80mm thermal printer)
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Biên nhận bán hàng 80mm', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nHẹn gặp lại!",
  "showAddress": true,
  "showTaxId": true,
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
    -- Product stamp: compact jewelry item label (16×30 mm)
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem trang sức 16x30mm', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": false,
  "labelWidth": 30,
  "labelHeight": 16
}', TRUE),
    -- Jewelry stamp — full label with gold type, brand, weight, price
    (current_setting('app.current_tenant', true), 'JEWELRY_STAMP', 'Tem vàng 16x30mm', '{
  "paperSize": "16x30mm",
  "fontSize": 7,
  "lineSpacing": 1.0,
  "fields": ["name", "goldType", "goldBrand", "weight", "price", "symbol", "counter"],
  "showBarcode": true,
  "showQrCode": false,
  "barcodeFormat": "CODE128"
}', TRUE),
    -- Jewelry stamp — compact QR label (12×20 mm)
    (current_setting('app.current_tenant', true), 'JEWELRY_STAMP', 'Tem vàng 12x20mm', '{
  "paperSize": "12x20mm",
  "fontSize": 6,
  "lineSpacing": 1.0,
  "fields": ["name", "goldType", "weight", "price"],
  "showBarcode": false,
  "showQrCode": true
}', FALSE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 7. Loyalty program ────────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 8. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,          1.00, '#CD7F32', 'Thành viên cơ bản',          1),
    (current_setting('app.current_tenant', true), 'Bạc',       10000000,   1.25, '#9E9E9E', 'Chi tiêu từ 10 triệu VNĐ',  2),
    (current_setting('app.current_tenant', true), 'Vàng',      50000000,   1.50, '#FFC107', 'Chi tiêu từ 50 triệu VNĐ',  3),
    (current_setting('app.current_tenant', true), 'Kim cương', 200000000,  2.00, '#00BCD4', 'Chi tiêu từ 200 triệu VNĐ', 4);

-- ── 9. Attribute groups & definitions — JEWELRY ───────────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'jewelry_info', 'Thông tin vàng', 1
FROM product_type WHERE code = 'JEWELRY'
AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'total_weight', 'Tổng trọng lượng (chỉ)', 'NUMBER', TRUE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gold_weight', 'Trọng lượng vàng (chỉ)', 'NUMBER', TRUE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gem_weight', 'Trọng lượng đá (chỉ)', 'NUMBER', FALSE, FALSE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'sell_by_item', 'Bán theo cái (không theo cân)', 'BOOLEAN', FALSE, FALSE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'proc_price', 'Phí gia công (VNĐ)', 'NUMBER', FALSE, FALSE, FALSE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'sell_proc_price', 'Phí gia công bán (VNĐ)', 'NUMBER', FALSE, FALSE, FALSE, 6
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'exchange_proc_price', 'Phí gia công đổi (VNĐ)', 'NUMBER', FALSE, FALSE, FALSE, 7
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gem_type', 'Loại đá quý', 'STRING', FALSE, TRUE, TRUE, 8
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'counter_code', 'Quầy trưng bày', 'STRING', FALSE, FALSE, TRUE, 9
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'symbol', 'Ký hiệu / Mã hàng', 'STRING', FALSE, TRUE, FALSE, 10
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'hallmark', 'Dấu kiểm định', 'STRING', FALSE, TRUE, FALSE, 11
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'certificate_number', 'Số chứng chỉ giám định', 'STRING', FALSE, TRUE, FALSE, 12
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'origin', 'Xuất xứ trang sức', 'STRING', FALSE, TRUE, TRUE, 13
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'jewelry_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, attribute_group_id = EXCLUDED.attribute_group_id, display_order = EXCLUDED.display_order;

-- ── 10. Attribute groups & definitions — ELECTRONICS ─────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'device_info', 'Thông tin thiết bị', 1
FROM product_type WHERE code = 'ELECTRONICS'
AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'condition_info', 'Tình trạng', 2
FROM product_type WHERE code = 'ELECTRONICS'
AND tenant_id = current_setting('app.current_tenant', true);

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

-- ── 11. Attribute groups & definitions — WATCH ───────────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'watch_info', 'Thông tin đồng hồ', 1
FROM product_type WHERE code = 'WATCH'
AND tenant_id = current_setting('app.current_tenant', true);

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

-- ── 12. Sample products ───────────────────────────────────────
-- 20 representative jewelry items for demo/testing.
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('JEWELRY-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name,
    seq.item_desc,
    seq.item_price,
    seq.item_cost,
    'chi',
    pt.id,
    'ACTIVE'
FROM (VALUES
    (1,  'Nhẫn nữ vàng 610',       'Nhẫn nữ vàng 610 - TKJ 0.45 chỉ',          3600000,  2970000),
    (2,  'Nhẫn nam vàng 9999',      'Nhẫn nam vàng 9999 - KNP 1.2 chỉ',        12500000, 11000000),
    (3,  'Lắc nữ vàng 610',         'Lắc nữ vàng 610 - PPJ 1.8 chỉ',           14400000, 12400000),
    (4,  'Dây chuyền vàng 750',     'Dây chuyền vàng 750 - SJC 1.0 chỉ',        4500000,  4100000),
    (5,  'Bông tai vàng 610',       'Bông tai vàng 610 - K*L* 0.6 chỉ/đôi',     4800000,  3900000),
    (6,  'Vòng em vàng 610',        'Vòng em vàng 610 - TKJ 0.3 chỉ',           2400000,  2100000),
    (7,  'Mặt dây chuyền 9999',     'Mặt dây chuyền vàng 9999 - KNP 0.5 chỉ',  4500000,  3900000),
    (8,  'Nhẫn cưới vàng 750',      'Nhẫn cưới vàng 750 - PNJ 1.0 chỉ',        4600000,  4200000),
    (9,  'Lắc tay bạc 925',         'Lắc tay bạc 925 - PTJ 5.0g',               650000,   450000),
    (10, 'Nhẫn bạc 925',            'Nhẫn bạc 925 - PTJ 3.0g',                  390000,   280000),
    (11, 'Dây chuyền bạc 925',      'Dây chuyền bạc 925 - PTJ 4.0g',            520000,   380000),
    (12, 'Bông tai bạc 925',        'Bông tai bạc 925 - PTJ 2.0g/đôi',          260000,   180000),
    (13, 'Nhẫn vàng 980',           'Nhẫn vàng 980 - VJC 0.8 chỉ',            6200000,  5800000),
    (14, 'Lắc vàng 9999',           'Lắc vàng 9999 - SJC 2.0 chỉ',           18000000, 16000000),
    (15, 'Mặt kiểu vàng 610',       'Mặt kiểu vàng 610 - K*L* 0.7 chỉ',       5600000,  4900000),
    (16, 'Vòng ximen vàng 610',     'Vòng ximen vàng 610 - MD 1.2 chỉ',        9600000,  8500000),
    (17, 'Nhẫn hột đá vàng 750',   'Nhẫn hột đá vàng 750 - PNJ 0.9 chỉ',      4100000,  3700000),
    (18, 'Dây chuyền vàng 9999',    'Dây chuyền vàng 9999 - KNP 1.5 chỉ',     13500000, 11800000),
    (19, 'Bông tai vàng 9999',      'Bông tai vàng 9999 - SJC 0.5 chỉ/đôi',   4500000,  3900000),
    (20, 'Lắc vàng 750',            'Lắc vàng 750 - TKJ 1.5 chỉ',             6800000,  6000000)
) AS seq(n, item_name, item_desc, item_price, item_cost)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'JEWELRY'
    AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 13. Inventory for sample products ─────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id,
    p.id,
    1, 0, 1,
    p.cost_price,
    'Quầy',
    FALSE
FROM product p
WHERE p.sku LIKE 'JEWELRY-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) DO NOTHING;

-- ── 14. Gold brand vendors ────────────────────────────────────
INSERT INTO vendors (tenant_id, code, name, address, notes, payment_terms) VALUES
    (current_setting('app.current_tenant', true), 'TKJ',   'CT TNHHVBTMDV TUẤN KIỆT',           '854-856 Trần Hưng Đạo, P.7, Q.5, TP.HCM',      'TKJ (Tuấn Kiệt)',    'NET_30'),
    (current_setting('app.current_tenant', true), 'KNP',   'CT TNHH VBDQ Kim Ngân Phát',         'ĐT746B, KP4, P.Hội Nghĩa, TU, BD',              'KNP (Kim Ngân Phát)', 'NET_30'),
    (current_setting('app.current_tenant', true), 'KLL',   'DNTN KDV KIM LOAN TUẤN',             '57 Nghĩa Thục, P.5, Q.5, TP.HCM',               'K*L* (Kim Loan Tuấn)', 'NET_30'),
    (current_setting('app.current_tenant', true), 'AHOA',  'CT TNHH VBĐQ HÒA HIẾU',             '151-155 Trần Tuấn Khải, P.5, Q.5, TP.HCM',      'A.HOA (Hòa Hiếu)',   'NET_30'),
    (current_setting('app.current_tenant', true), 'PPJ',   'CT TNHH KDVBDQ Kim Sen',             '5 Chiêu Anh Các, P.5, Q.5, TP.HCM',             'PPJ (Kim Sen)',       'NET_30'),
    (current_setting('app.current_tenant', true), 'VJC',   'CT TNHH MTV VBĐQ Tp HCM',            '3-5 Hồ Tùng Mậu, P.Ng.Thị Bình, Q.1, TP.HCM',  'VJC (Sài Gòn)',      'NET_30'),
    (current_setting('app.current_tenant', true), 'MD',    'DNTN TMVTS MI DUNG',                  '29 Nhiêu Tâm, P.5, Q.5, TP.HCM',                'MD (Mỹ Dung)',        'NET_30'),
    (current_setting('app.current_tenant', true), 'TD',    'CTY TNHH KDVB Tân Thanh Danh',        '7A An Dương Vương, P.8, Q.5, TP.HCM',           'TD (Tân Thanh Danh)', 'NET_30'),
    (current_setting('app.current_tenant', true), 'KHD',   'Cty TNHH MTV SX TMDV Vàng Thế Hùng', '40 Võ Văn Tần, P.2, TP.Tân An, Long An',        'KHD (Thế Hùng)',     'NET_30'),
    (current_setting('app.current_tenant', true), 'KHS',   'DNTN Vàng Kim Hoàn Sơn',             '66 Bùi Hữu Nghĩa, P.5, Q.5, TP.HCM',            'KHS (Kim Hoàn Sơn)', 'NET_30'),
    (current_setting('app.current_tenant', true), 'KHA',   'CTY TNHH KDVBDQ Kim Hảo',            '11 Nhiêu Tâm, P.5, Q.5, TP.HCM',                'KH* (Kim Hảo)',       'NET_30'),
    (current_setting('app.current_tenant', true), 'KM',    'Cty TNHH Kim Mai Bùi Hữu Nghĩa',     '85-87 Bùi Hữu Nghĩa, P.5, Q.5, TP.HCM',        'KM (Kim Mai BHN)',    'NET_30'),
    (current_setting('app.current_tenant', true), 'KLMK',  'Cty TNHH MTV Kim Long Mekong',        '18 Nhiêu Tâm, P.5, Q.5, TP.HCM',                'KLMK (Kim Long Mekong)', 'NET_30'),
    (current_setting('app.current_tenant', true), 'PNJ',   'Cty CP VBDQ PNJ Phú Nhuận',           '170E Phan Đăng Lưu, Q.Phú Nhuận, TP.HCM',      'PNJ (Phú Nhuận)',    'NET_30'),
    (current_setting('app.current_tenant', true), 'SJC',   'Công ty TNHH MTV VBĐQ Sài Gòn',      '418-420 Nguyễn Thị Minh Khai, Q.3, TP.HCM',    'SJC',                 'NET_30'),
    (current_setting('app.current_tenant', true), 'TTR',   'Công ty VBĐQ Thanh Trúc',             '190A Dương Bá Trạc, P.2, Q.8, TP.HCM',          'TT* (Thanh Trúc)',   'NET_30'),
    (current_setting('app.current_tenant', true), 'PTJ',   'Bạc Phú Thanh',                       '64 Nghĩa Thục, P.5, Q.5, TP.HCM',               'PTJ (Phú Thanh)',    'NET_30')
ON CONFLICT (code, tenant_id) DO NOTHING;
