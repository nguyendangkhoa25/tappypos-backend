-- ============================================================
-- TENANT SEED — DEFAULT DATA: JEWELRY SHOP (TIỆM VÀNG)
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
    (current_setting('app.current_tenant', true), 'HEALTH',       'Sức khỏe / Dinh dưỡng',     'Sản phẩm sức khỏe và dinh dưỡng'),
    (current_setting('app.current_tenant', true), 'JEWELRY',      'Vàng bạc đá quý',           'Nữ trang, vàng, bạc, đá quý'),
    (current_setting('app.current_tenant', true), 'WATCH',        'Đồng hồ',                   'Đồng hồ đeo tay và đồng hồ bàn'),
    (current_setting('app.current_tenant', true), 'OTHER',        'Khác',                      'Các mặt hàng khác')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;

-- ── 2. Categories ─────────────────────────────────────────────
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Vàng bạc đá quý', NULL),
    (current_setting('app.current_tenant', true), 'Điện tử',          NULL),
    (current_setting('app.current_tenant', true), 'Xe',               NULL),
    (current_setting('app.current_tenant', true), 'Đồ gia dụng',      NULL),
    (current_setting('app.current_tenant', true), 'Khác',             NULL);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Vàng 9999 / 24K',   'Vàng bạc đá quý'),
    ('Vàng 610 / 14K',    'Vàng bạc đá quý'),
    ('Vàng 750 / 18K',    'Vàng bạc đá quý'),
    ('Vàng 980 / 980',    'Vàng bạc đá quý'),
    ('Vàng 990',          'Vàng bạc đá quý'),
    ('Bạc',               'Vàng bạc đá quý'),
    ('Đồng hồ đeo tay',   'Vàng bạc đá quý'),
    ('Đá quý / Khác',     'Vàng bạc đá quý'),
    ('Điện thoại',        'Điện tử'),
    ('Laptop / Máy tính', 'Điện tử'),
    ('Máy tính bảng',     'Điện tử'),
    ('Thiết bị âm thanh', 'Điện tử'),
    ('Xe máy',            'Xe'),
    ('Xe đạp điện',       'Xe')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Walk-in customer ───────────────────────────────────────
INSERT INTO customers (tenant_id, name, phone, email, notes, deleted)
VALUES (current_setting('app.current_tenant', true), 'Khách lẻ', '0000000000', NULL,
        'Khách hàng lẻ - không có thông tin liên hệ', FALSE)
ON CONFLICT (phone, tenant_id) DO NOTHING;

-- ── 4. Gold types (tuổi vàng catalog — global reference) ──────
INSERT INTO gold_types (code, label, is_silver, sort_order) VALUES
    ('Unknown', 'Tuổi vàng không xác định', FALSE,  0),
    ('610',     'Vàng 610',                 FALSE,  1),
    ('9999',    'Vàng 9999',                FALSE,  2),
    ('680',     'Vàng 680',                 FALSE,  3),
    ('750',     'Vàng 750',                 FALSE,  4),
    ('950',     'Vàng 950',                 FALSE,  5),
    ('980',     'Vàng 980',                 FALSE,  6),
    ('990',     'Vàng 990',                 FALSE,  7),
    ('10K',     'Vàng 10K',                 FALSE,  8),
    ('23K',     'Vàng 23K',                 FALSE,  9),
    ('17K',     'Vàng 17K',                 FALSE, 10),
    ('600',     'Vàng 600',                 FALSE, 11),
    ('16K',     'Vàng 16K',                 FALSE, 12),
    ('15K',     'Vàng 15K',                 FALSE, 13),
    ('B925',    'Bạc 925',                  TRUE,  14),
    ('B950',    'Bạc 950',                  TRUE,  15),
    ('Bac9999', 'Bạc 9999',                 TRUE,  16)
ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order;

-- ── 5. Gold brands (chành vàng catalog — global reference) ────
INSERT INTO gold_brands (code, label, name, short_name, pub_stand, address, origin, is_silver, sort_order) VALUES
    ('Unknown', 'Chành không xác định',      'Unknown',                           'Unknown',        'Unknown',        'Unknown',                                       'vn',      FALSE,  0),
    ('TKJ',     'TKJ (Tuấn Kiệt)',           'CT TNHHVBTMDV TUẤN KIỆT',          'TUẤN KIỆT',      '01:2024/TKJ',    '854-856 Trần Hưng Đạo, P.7, Q.5, TP.HCM',     'Việt Nam', FALSE,  1),
    ('KNP',     'KNP (Kim Ngân Phát)',       'CT TNHH VBDQ Kim Ngân Phát',       'KIM NGÂN PHÁT',  '01:2023/KNP',    'ĐT746B, KP4, P.Hội Nghĩa, TU, BD',             'Việt Nam', FALSE,  2),
    ('K*L*',    'K*L* (Kim Loan Tuấn)',      'DNTN KDV KIM LOAN TUẤN',           'KIM LOAN TUẤN',  '01:2023/K*L*',   '57 Nghĩa Thục, P.5, Q.5, TP.HCM',              'Việt Nam', FALSE,  3),
    ('A.HOA',   'A.HOA (Hòa Hiếu)',          'CT TNHH VBĐQ HÒA HIẾU',           'HÒA HIẾU',       '01:2023/HH',     '151-155 Trần Tuấn Khải, P.5, Q.5, TP.HCM',     'Việt Nam', FALSE,  4),
    ('PPJ',     'PPJ (Kim Sen)',             'CT TNHH KDVBDQ Kim Sen',           'KIM SEN',        '01:2023/PPJ',    '5 Chiêu Anh Các, P.5, Q.5, TP.HCM',            'Việt Nam', FALSE,  5),
    ('VJC',     'VJC (Sài Gòn)',            'CT TNHH MTV VBĐQ Tp HCM',          'VBĐQ SÀI GÒN',   '01:2023/VJC',    '3-5 Hồ Tùng Mậu, P.Ng.Thị Bình, Q.1, TP.HCM','Việt Nam', FALSE,  6),
    ('MD',      'MD (Mỹ Dung)',             'DNTN TMVTS MI DUNG',               'MỸ DUNG',        '01:2023/MD',     '29 Nhiêu Tâm, P.5, Q.5, TP.HCM',               'Việt Nam', FALSE,  7),
    ('TD',      'TD (Tân Thanh Danh)',      'CTY TNHH KDVB Tân Thanh Danh',     'TÂN THANH DANH', '01:2023/TTD',    '7A An Dương Vương, P.8, Q.5, TP.HCM',          'Việt Nam', FALSE,  8),
    ('KHD',     'KHD (Thế Hùng)',           'Cty TNHH MTV SX TMDV Vàng Thế Hùng','THẾ HÙNG',     '01:2023/KHD',    '40 Võ Văn Tần, P.2, TP.Tân An, Long An',       'Việt Nam', FALSE,  9),
    ('KHS',     'KHS (Kim Hoàn Sơn)',       'DNTN Vàng Kim Hoàn Sơn',           'KIM HOÀN SƠN',   '01:2023/KHS',    '66 Bùi Hữu Nghĩa, P.5, Q.5, TP.HCM',          'Việt Nam', FALSE, 10),
    ('KH*',     'KH* (Kim Hảo)',            'CTY TNHH KDVBDQ Kim Hảo',          'KIM HẢO',        '01:2023/KH*',    '11 Nhiêu Tâm, P.5, Q.5, TP.HCM',              'Việt Nam', FALSE, 11),
    ('KM',      'KM (Kim Mai BHN)',         'Cty TNHH Kim Mai Bùi Hữu Nghĩa',  'KIM MAI BHN',    '04:2023/KMBHN',  '85-87 Bùi Hữu Nghĩa, P.5, Q.5, TP.HCM',       'Việt Nam', FALSE, 20),
    ('KLMK',    'KLMK (Kim Long Mekong)',  'Cty TNHH MTV Kim Long Mekong',     'KIM LONG MEKONG','01:2023/KLMK',   '18 Nhiêu Tâm, P.5, Q.5, TP.HCM',              'Việt Nam', FALSE, 12),
    ('PNJ',     'PNJ (Phú Nhuận)',         'Cty CP VBDQ PNJ Phú Nhuận',        'PNJ',            '01:2023/PNJ',    '170E Phan Đăng Lưu, Q.Phú Nhuận, TP.HCM',      'Việt Nam', FALSE, 14),
    ('SJC',     'SJC (VBĐQ Sài Gòn)',      'Công ty TNHH MTV VBĐQ Sài Gòn',   'SJC',            '01:2023/SJC',    '418-420 Nguyễn Thị Minh Khai, Q.3, TP.HCM',    'Việt Nam', FALSE, 15),
    ('TT*',     'TT* (Thanh Trúc)',        'Công ty VBĐQ Thanh Trúc',          'THANH TRÚC',     '01:2023/TT',     '190A Dương Bá Trạc, P.2, Q.8, TP.HCM',         'Việt Nam', FALSE, 16),
    ('PTJ',     'PT* (Phú Thanh)',         'Bạc Phú Thanh',                    'PHÚ THANH',      '01:2023/PTJ',    '64 Nghĩa Thục, P.5, Q.5, TP.HCM',              'Việt Nam', TRUE,  23)
ON CONFLICT (code) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order;

-- ── 6. Jewelry display counters (quầy trưng bày) ──────────────
INSERT INTO jewelry_counters (tenant_id, code, name, sort_order) VALUES
    (current_setting('app.current_tenant', true), 'Unknown',    'Chọn quầy',         0),
    (current_setting('app.current_tenant', true), 'BONG',       'Quầy bông',         1),
    (current_setting('app.current_tenant', true), 'BONGTREO',   'Quầy bông treo',    2),
    (current_setting('app.current_tenant', true), 'NHAN NU',    'Quầy nhẫn nữ',     3),
    (current_setting('app.current_tenant', true), 'NHAN NAM',   'Quầy nhẫn nam',     4),
    (current_setting('app.current_tenant', true), 'NHAN CUOI',  'Quầy nhẫn cặp',    5),
    (current_setting('app.current_tenant', true), 'LAC',        'Quầy lắc',          6),
    (current_setting('app.current_tenant', true), 'XIMEN',      'Quầy vòng ximen',   7),
    (current_setting('app.current_tenant', true), 'VONG EM',    'Quầy vòng em',      8),
    (current_setting('app.current_tenant', true), 'DAY CHUYEN', 'Quầy dây chuyền',  9),
    (current_setting('app.current_tenant', true), 'MAT KIEU',   'Quầy mặt kiểu',    10),
    (current_setting('app.current_tenant', true), 'NHAN TRON',  'Quầy nhẫn trơn',   11)
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 7. Gold prices (giá vàng theo tuổi) ──────────────────────
-- Prices in VNĐ/chỉ (1 chỉ = 3.75g). Update daily in production.
INSERT INTO gold_price (tenant_id, code, label, pawn, sell, buy, show_in_board, display_order) VALUES
    (current_setting('app.current_tenant', true), '9999',    'Vàng 9999',  8000000,  9040000, 8780000, TRUE,   1),
    (current_setting('app.current_tenant', true), '610',     'Vàng 610',   7300000,  9120000, 8440000, TRUE,   2),
    (current_setting('app.current_tenant', true), '980',     'Vàng 980',   6800000,  7660000, 7500000, TRUE,   3),
    (current_setting('app.current_tenant', true), '750',     'Vàng 750',   4000000,  4550000, 4150000, TRUE,   4),
    (current_setting('app.current_tenant', true), '950',     'Vàng 950',   5000000,  6540000, 6340000, FALSE,  5),
    (current_setting('app.current_tenant', true), '990',     'Vàng 990',   6500000,  7240000, 7140000, FALSE,  6),
    (current_setting('app.current_tenant', true), '680',     'Vàng 680',   3900000,  4580000, 4200000, FALSE,  7),
    (current_setting('app.current_tenant', true), '23K',     'Vàng 23K',   5000000,  6600000, 6420000, FALSE,  8),
    (current_setting('app.current_tenant', true), '17K',     'Vàng 17K',   3400000,  4150000, 3800000, FALSE,  9),
    (current_setting('app.current_tenant', true), '15K',     'Vàng 15K',   3400000,  4150000, 3800000, FALSE, 10),
    (current_setting('app.current_tenant', true), '16K',     'Vàng 16K',   3400000,  4150000, 3800000, FALSE, 11),
    (current_setting('app.current_tenant', true), '600',     'Vàng 600',   4000000,  4500000, 3800000, FALSE, 12),
    (current_setting('app.current_tenant', true), '10K',     'Vàng 10K',         0,        0,       0, FALSE, 13),
    (current_setting('app.current_tenant', true), 'B925',    'Bạc 925',          0,   130000,   65000, FALSE, 14),
    (current_setting('app.current_tenant', true), 'B950',    'Bạc 950',          0,   130000,   65000, FALSE, 15),
    (current_setting('app.current_tenant', true), 'Bac9999', 'Bạc 9999',         0,        0,       0, FALSE, 16)
ON CONFLICT (code, tenant_id) DO UPDATE
    SET sell = EXCLUDED.sell, buy = EXCLUDED.buy, pawn = EXCLUDED.pawn, label = EXCLUDED.label;

-- ── 8. Shop configuration ─────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'PAWN_INTEREST_VALUE',       '3',       'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'PAWN_EXPIRATION_DAYS',      '45',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'PAWN_INTEREST_CALCULATION', '30',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'PRICE_BOARD_CODE',          '6868689', 'DISPLAY', FALSE),
    (current_setting('app.current_tenant', true), 'JEWELRY_STAMP_TYPE',        '16x30',   'PRINT',   FALSE),
    (current_setting('app.current_tenant', true), 'EXCLUDE_VISIBLE_ITEM',      'NO',      'DISPLAY', FALSE),
    (current_setting('app.current_tenant', true), 'cash_denominations',        '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS',  FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_rate',        '0.0',     'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_interest_type',        '30',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_due_date',             '30',      'PAWN',    FALSE),
    (current_setting('app.current_tenant', true), 'pawn_exclude_visible_item', 'false',   'PAWN',    FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 9. Print templates ────────────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'RECEIPT', 'Biên nhận 80mm', '{
  "paperSize": "80mm",
  "showLogo": false,
  "showAddress": true,
  "showPhone": true,
  "showTaxCode": true,
  "showQrCode": false,
  "fontSize": 12,
  "lineSpacing": 1.2,
  "headerLines": ["{{shopName}}", "{{address}}", "ĐT: {{phone}}", "MST: {{taxCode}}"],
  "footerLines": ["Cảm ơn quý khách!", "Hẹn gặp lại!"],
  "showOrderNumber": true,
  "showCashier": true,
  "showPaymentMethod": true,
  "showChangeAmount": true,
  "itemColumns": ["name", "qty", "price", "total"]
}', TRUE),
    (current_setting('app.current_tenant', true), 'JEWELRY_STAMP', 'Tem vàng 16x30mm', '{
  "paperSize": "16x30mm",
  "fontSize": 7,
  "lineSpacing": 1.0,
  "fields": ["name", "goldType", "goldBrand", "weight", "price", "symbol", "counter"],
  "showBarcode": true,
  "showQrCode": false,
  "barcodeFormat": "CODE128"
}', TRUE),
    (current_setting('app.current_tenant', true), 'JEWELRY_STAMP', 'Tem vàng 12x20mm', '{
  "paperSize": "12x20mm",
  "fontSize": 6,
  "lineSpacing": 1.0,
  "fields": ["name", "goldType", "weight", "price"],
  "showBarcode": false,
  "showQrCode": true
}', FALSE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 10. Loyalty program ───────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 11. Loyalty tiers ─────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,          1.00, '#CD7F32', 'Thành viên cơ bản',          1),
    (current_setting('app.current_tenant', true), 'Bạc',       10000000,   1.25, '#9E9E9E', 'Chi tiêu từ 10 triệu VNĐ',  2),
    (current_setting('app.current_tenant', true), 'Vàng',      50000000,   1.50, '#FFC107', 'Chi tiêu từ 50 triệu VNĐ',  3),
    (current_setting('app.current_tenant', true), 'Kim cương', 200000000,  2.00, '#00BCD4', 'Chi tiêu từ 200 triệu VNĐ', 4);

-- ── 12. Attribute groups & definitions — JEWELRY ──────────────

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'material_info', 'Thông tin vật liệu', 1
FROM product_type WHERE code = 'JEWELRY'
AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'weight_info', 'Trọng lượng', 2
FROM product_type WHERE code = 'JEWELRY'
AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'pricing_info', 'Giá & phí gia công', 3
FROM product_type WHERE code = 'JEWELRY'
AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'location_info', 'Vị trí trưng bày', 4
FROM product_type WHERE code = 'JEWELRY'
AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'cert_info', 'Giám định & chứng chỉ', 5
FROM product_type WHERE code = 'JEWELRY'
AND tenant_id = current_setting('app.current_tenant', true);

-- Attribute definitions — material_info
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gold_type_code', 'Tuổi vàng', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'material_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gold_brand_code', 'Chành vàng', 'STRING', FALSE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'material_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'is_silver', 'Hàng bạc', 'BOOLEAN', FALSE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'material_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'gem_type', 'Loại đá quý', 'STRING', FALSE, TRUE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'material_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute definitions — weight_info
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
    'sell_by_item', 'Bán theo cái (không theo cân)', 'BOOLEAN', FALSE, FALSE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'weight_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute definitions — pricing_info
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'proc_price', 'Phí gia công (VNĐ)', 'NUMBER', FALSE, FALSE, FALSE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'pricing_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'exchange_proc_price', 'Phí gia công đổi (VNĐ)', 'NUMBER', FALSE, FALSE, FALSE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'pricing_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'sell_proc_price', 'Phí gia công bán (VNĐ)', 'NUMBER', FALSE, FALSE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'pricing_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute definitions — location_info
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'counter_code', 'Quầy trưng bày', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'location_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'symbol', 'Ký hiệu / Mã hàng', 'STRING', FALSE, TRUE, FALSE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'location_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute definitions — cert_info
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'hallmark', 'Dấu kiểm định', 'STRING', FALSE, TRUE, FALSE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cert_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'certificate_number', 'Số chứng chỉ giám định', 'STRING', FALSE, TRUE, FALSE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cert_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id,
    'origin', 'Xuất xứ trang sức', 'STRING', FALSE, TRUE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cert_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
WHERE pt.code = 'JEWELRY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 13. Attribute groups & definitions — ELECTRONICS ──────────

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

-- ── 14. Attribute groups & definitions — WATCH ────────────────

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

-- ── 15. Sample products ───────────────────────────────────────
-- 20 representative jewelry items for demo/testing.
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('KNP-DEMO-', LPAD(seq.n::text, 4, '0')),
    seq.item_name,
    seq.item_desc,
    seq.item_price,
    seq.item_cost,
    'cái',
    pt.id,
    'ACTIVE'
FROM (VALUES
    (1,  'Nhẫn nữ vàng 610',       'Nhẫn nữ vàng 610 - TKJ 0.45 chỉ',           3600000,  2970000),
    (2,  'Nhẫn nam vàng 9999',      'Nhẫn nam vàng 9999 - KNP 1.2 chỉ',         12500000, 11000000),
    (3,  'Lắc nữ vàng 610',         'Lắc nữ vàng 610 - PPJ 1.8 chỉ',            14400000, 12400000),
    (4,  'Dây chuyền vàng 750',     'Dây chuyền vàng 750 - SJC 1.0 chỉ',         4500000,  4100000),
    (5,  'Bông tai vàng 610',       'Bông tai vàng 610 - K*L* 0.6 chỉ/đôi',      4800000,  3900000),
    (6,  'Vòng em vàng 610',        'Vòng em vàng 610 - TKJ 0.3 chỉ',            2400000,  2100000),
    (7,  'Mặt dây chuyền 9999',     'Mặt dây chuyền vàng 9999 - KNP 0.5 chỉ',   4500000,  3900000),
    (8,  'Nhẫn cưới vàng 750',      'Nhẫn cưới vàng 750 - PNJ 1.0 chỉ',         4600000,  4200000),
    (9,  'Lắc tay bạc 925',         'Lắc tay bạc 925 - PTJ 5.0g',                650000,   450000),
    (10, 'Nhẫn bạc 925',            'Nhẫn bạc 925 - PTJ 3.0g',                   390000,   280000),
    (11, 'Dây chuyền bạc 925',      'Dây chuyền bạc 925 - PTJ 4.0g',             520000,   380000),
    (12, 'Bông tai bạc 925',        'Bông tai bạc 925 - PTJ 2.0g/đôi',           260000,   180000),
    (13, 'Nhẫn vàng 980',           'Nhẫn vàng 980 - VJC 0.8 chỉ',             6200000,  5800000),
    (14, 'Lắc vàng 9999',           'Lắc vàng 9999 - SJC 2.0 chỉ',            18000000, 16000000),
    (15, 'Mặt kiểu vàng 610',       'Mặt kiểu vàng 610 - K*L* 0.7 chỉ',        5600000,  4900000),
    (16, 'Vòng ximen vàng 610',     'Vòng ximen vàng 610 - MD 1.2 chỉ',         9600000,  8500000),
    (17, 'Nhẫn hột đá vàng 750',   'Nhẫn hột đá vàng 750 - PNJ 0.9 chỉ',       4100000,  3700000),
    (18, 'Dây chuyền vàng 9999',    'Dây chuyền vàng 9999 - KNP 1.5 chỉ',      13500000, 11800000),
    (19, 'Bông tai vàng 9999',      'Bông tai vàng 9999 - SJC 0.5 chỉ/đôi',    4500000,  3900000),
    (20, 'Lắc vàng 750',            'Lắc vàng 750 - TKJ 1.5 chỉ',              6800000,  6000000)
) AS seq(n, item_name, item_desc, item_price, item_cost)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'JEWELRY'
    AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 16. Inventory for sample products ─────────────────────────
INSERT INTO inventory (product_id, quantity)
SELECT p.id, 1
FROM product p
WHERE p.sku LIKE 'KNP-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) DO NOTHING;
