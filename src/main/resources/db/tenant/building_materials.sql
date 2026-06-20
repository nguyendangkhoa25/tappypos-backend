-- ============================================================
-- TENANT SEED — DEFAULT DATA: BUILDING MATERIALS (VẬT LIỆU XÂY DỰNG / VLXD)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- Primary product type: HARDWARE (Đồ sắt / Vật liệu).
-- ============================================================

-- ── 1. Product types (standard set; HARDWARE is primary) ──────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'HARDWARE',     'Vật liệu / Đồ sắt',          'Vật liệu xây dựng và đồ sắt', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Hàng tiêu dùng',             'Hàng tiêu dùng thiết yếu', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'ELECTRONICS',  'Điện tử',                    'Thiết bị điện', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FURNITURE',    'Đồ nội thất',                'Nội thất gia đình', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'AUTO_PARTS',   'Phụ tùng',                   'Phụ tùng và phụ kiện', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'APPLIANCES',   'Đồ gia dụng',                'Thiết bị gia dụng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'OFFICE',       'Văn phòng phẩm',             'Đồ dùng văn phòng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'SERVICE',      'Dịch vụ',                    'Dịch vụ (giao hàng, gia công…)', 'NO_INVENTORY', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ─────────────────────────────────────────────
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Xi măng',                NULL),
    (current_setting('app.current_tenant', true), 'Sắt thép',               NULL),
    (current_setting('app.current_tenant', true), 'Gạch / Ngói',            NULL),
    (current_setting('app.current_tenant', true), 'Cát đá',                 NULL),
    (current_setting('app.current_tenant', true), 'Sơn',                    NULL),
    (current_setting('app.current_tenant', true), 'Ống nước & Phụ kiện',    NULL),
    (current_setting('app.current_tenant', true), 'Điện',                   NULL),
    (current_setting('app.current_tenant', true), 'Dụng cụ',                NULL);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Xi măng dân dụng',        'Xi măng'),
    ('Bột trét / Vữa',          'Xi măng'),
    ('Thép xây dựng (cây)',     'Sắt thép'),
    ('Thép hộp / Thép hình',    'Sắt thép'),
    ('Tôn lợp',                 'Sắt thép'),
    ('Gạch xây',                'Gạch / Ngói'),
    ('Gạch ốp lát',             'Gạch / Ngói'),
    ('Ngói lợp',                'Gạch / Ngói'),
    ('Cát',                     'Cát đá'),
    ('Đá',                      'Cát đá'),
    ('Sơn nước',                'Sơn'),
    ('Sơn lót / Chống thấm',    'Sơn'),
    ('Ống nhựa PVC',            'Ống nước & Phụ kiện'),
    ('Phụ kiện ống',            'Ống nước & Phụ kiện'),
    ('Dây điện',                'Điện'),
    ('Thiết bị điện',           'Điện'),
    ('Dụng cụ cầm tay',         'Dụng cụ'),
    ('Dụng cụ điện',            'Dụng cụ')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà phân phối xi măng & gạch',        'VND-VLXD-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp sắt thép & tôn',          'VND-VLXD-002', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp sơn, ống nước & điện',    'VND-VLXD-003', NULL, NULL, 'NET_15', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5. Sample products ────────────────────────────────────────
-- Starter catalogue so the shop has something sellable on day one. All HARDWARE,
-- using construction units (bao / cây / viên / m2 / m3 / tấm / roll / kg).
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('VLXD-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1,  'Xi măng Hà Tiên PCB40',          'Xi măng PCB40 bao 50kg',                 95000,   82000,  'bao',   'Xi măng'),
    (2,  'Xi măng Holcim PCB40',           'Xi măng PCB40 bao 50kg',                 98000,   85000,  'bao',   'Xi măng'),
    (3,  'Bột trét tường ngoại thất',      'Bột trét tường bao 40kg',               220000,  185000,  'bao',   'Xi măng'),
    (4,  'Thép cuộn Việt Nhật phi 6',      'Thép xây dựng phi 6',                    25000,   21000,  'cay',   'Sắt thép'),
    (5,  'Thép Hòa Phát phi 16',           'Thép cây vằn phi 16, dài 11.7m',        165000,  145000,  'cay',   'Sắt thép'),
    (6,  'Thép hộp mạ kẽm 30x60',          'Thép hộp 30x60 dày 1.4mm',              120000,  100000,  'cay',   'Sắt thép'),
    (7,  'Tôn lạnh màu sóng vuông',        'Tôn lợp mái màu, khổ 1.07m',             95000,   78000,  'm2',    'Sắt thép'),
    (8,  'Gạch ống Tuynel 8x8x18',         'Gạch ống xây tường',                      1200,     900,  'vien',  'Gạch / Ngói'),
    (9,  'Gạch thẻ đặc',                   'Gạch thẻ đặc xây tường chịu lực',         1500,    1100,  'vien',  'Gạch / Ngói'),
    (10, 'Gạch men lát nền 60x60',         'Gạch ốp lát men bóng 60x60',            185000,  150000,  'm2',    'Gạch / Ngói'),
    (11, 'Ngói màu Đồng Tâm',              'Ngói lợp màu',                            9000,    7000,  'vien',  'Gạch / Ngói'),
    (12, 'Cát xây tô',                     'Cát vàng xây tô',                       350000,  300000,  'm3',    'Cát đá'),
    (13, 'Đá 1x2',                         'Đá xây dựng 1x2',                       420000,  360000,  'm3',    'Cát đá'),
    (14, 'Sơn nước Dulux nội thất 18L',    'Sơn nước nội thất thùng 18L',          1850000, 1600000,  'box',   'Sơn'),
    (15, 'Sơn lót chống kiềm 5L',          'Sơn lót chống kiềm thùng 5L',           650000,  550000,  'box',   'Sơn'),
    (16, 'Ống nhựa PVC Bình Minh phi 90',  'Ống nhựa PVC phi 90, dài 4m',            85000,   70000,  'cay',   'Ống nước & Phụ kiện'),
    (17, 'Co nối PVC phi 90',              'Co 90 độ PVC phi 90',                    12000,    8000,  'piece', 'Ống nước & Phụ kiện'),
    (18, 'Dây điện Cadivi 2.5mm',          'Dây điện đơn 2.5mm cuộn 100m',          850000,  720000,  'roll',  'Điện'),
    (19, 'Ổ cắm điện đôi',                 'Ổ cắm điện âm tường loại đôi',           45000,   30000,  'piece', 'Điện'),
    (20, 'Máy khoan cầm tay Bosch',        'Máy khoan động lực cầm tay',           1450000, 1200000,  'piece', 'Dụng cụ'),
    (21, 'Búa đóng đinh',                  'Búa đóng đinh cán gỗ',                   65000,   45000,  'piece', 'Dụng cụ'),
    (22, 'Đinh xây dựng các loại',         'Đinh thép bán theo cân',                 25000,   18000,  'kg',    'Dụng cụ')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit, cat_name)
JOIN product_type pt
    ON pt.code = 'HARDWARE'
   AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 6. Product → category links (to parent categories) ────────
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('VLXD-DEMO-001', 'Xi măng'),
    ('VLXD-DEMO-002', 'Xi măng'),
    ('VLXD-DEMO-003', 'Xi măng'),
    ('VLXD-DEMO-004', 'Sắt thép'),
    ('VLXD-DEMO-005', 'Sắt thép'),
    ('VLXD-DEMO-006', 'Sắt thép'),
    ('VLXD-DEMO-007', 'Sắt thép'),
    ('VLXD-DEMO-008', 'Gạch / Ngói'),
    ('VLXD-DEMO-009', 'Gạch / Ngói'),
    ('VLXD-DEMO-010', 'Gạch / Ngói'),
    ('VLXD-DEMO-011', 'Gạch / Ngói'),
    ('VLXD-DEMO-012', 'Cát đá'),
    ('VLXD-DEMO-013', 'Cát đá'),
    ('VLXD-DEMO-014', 'Sơn'),
    ('VLXD-DEMO-015', 'Sơn'),
    ('VLXD-DEMO-016', 'Ống nước & Phụ kiện'),
    ('VLXD-DEMO-017', 'Ống nước & Phụ kiện'),
    ('VLXD-DEMO-018', 'Điện'),
    ('VLXD-DEMO-019', 'Điện'),
    ('VLXD-DEMO-020', 'Dụng cụ'),
    ('VLXD-DEMO-021', 'Dụng cụ'),
    ('VLXD-DEMO-022', 'Dụng cụ')
) AS m(sku, cat)
JOIN product p ON p.sku = m.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = m.cat AND c.parent_id IS NULL
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, category_id) DO NOTHING;

-- ── 7. Inventory for sample products ──────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 100, 20, 50, p.cost_price, 'Kho vật liệu', FALSE
FROM product p
WHERE p.sku LIKE 'VLXD-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) WHERE variant_id IS NULL AND deleted = false DO NOTHING;

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
    (current_setting('app.current_tenant', true), 'Đồng',      0,          1.00, '#CD7F32', 'Thành viên cơ bản',          1),
    (current_setting('app.current_tenant', true), 'Bạc',       10000000,   1.25, '#9E9E9E', 'Chi tiêu từ 10 triệu VND',   2),
    (current_setting('app.current_tenant', true), 'Vàng',      50000000,   1.50, '#FFC107', 'Chi tiêu từ 50 triệu VND',   3),
    (current_setting('app.current_tenant', true), 'Kim cương', 200000000,  2.00, '#00BCD4', 'Chi tiêu từ 200 triệu VND',  4);

-- ── 10. Default print templates ───────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    -- POS receipt: show tax + customer (VLXD often bills contractors with VAT)
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Mặc định', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nHẹn gặp lại!",
  "showAddress": true,
  "showTaxId": true,
  "showOrderNumber": true,
  "showDateTime": true,
  "showCustomer": true,
  "showTaxBreakdown": true,
  "showCashDetails": true,
  "paperWidth": "80mm",
  "autoClose": true
}', TRUE),
    -- Product stamp: barcode + price, no expiry (building materials do not expire)
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem sản phẩm', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": false,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE),
    -- Inventory stamp: batch + location for stock management
    (current_setting('app.current_tenant', true), 'INVENTORY_STAMP', 'Tem kho', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": false,
  "showBarcode": true,
  "showLocation": true,
  "showBatch": true,
  "showExpiry": false,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups & definitions (HARDWARE type) ────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'basic_info', 'Thông tin cơ bản', 1
FROM product_type WHERE code = 'HARDWARE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'specifications', 'Quy cách & Tiêu chuẩn', 2
FROM product_type WHERE code = 'HARDWARE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET display_order = EXCLUDED.display_order;

-- Group 1: basic_info
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Thương hiệu / Hãng', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'model', 'Quy cách / Mã hàng', 'STRING', FALSE, TRUE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'barcode_upc', 'Mã vạch / Barcode', 'STRING', FALSE, TRUE, FALSE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Group 2: specifications
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'standard_grade', 'Mác / Tiêu chuẩn (PCB40, CB300…)', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'specifications'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'dimension_spec', 'Kích thước / Quy cách (phi, dày, khổ)', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'specifications'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'color', 'Màu sắc', 'STRING', FALSE, FALSE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'specifications'
WHERE pt.code = 'HARDWARE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
