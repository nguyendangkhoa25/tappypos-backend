-- ============================================================
-- TENANT SEED — DEFAULT DATA: VEHICLE_SHOP (CỬA HÀNG XE — XE MÁY / XE ĐẠP ĐIỆN / XE ĐẠP)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING/DO UPDATE or WHERE NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
--
-- Dominant product types: MOTORBIKE / E_BIKE / BICYCLE — each a UNIQUE titled unit tracked
-- by số khung (chassis_number) + số máy (engine_number) on its vehicle_unit row. Parts are
-- TRACKED, repair labor is NO_INVENTORY. The "Phiếu giao xe" POS_RECEIPT default is seeded in
-- Java by TenantSeedService.seedShopTypeTemplates(); it is intentionally NOT seeded here.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'MOTORBIKE',   'Xe máy',                   'Xe máy số / tay ga, mới và cũ — mỗi chiếc một số khung/số máy', 'UNIQUE', 'piece'),
    (current_setting('app.current_tenant', true), 'E_BIKE',      'Xe đạp điện / Xe máy điện','Xe đạp điện, xe máy điện — mỗi chiếc một số khung', 'UNIQUE', 'piece'),
    (current_setting('app.current_tenant', true), 'BICYCLE',     'Xe đạp',                   'Xe đạp các loại — mỗi chiếc một số khung', 'UNIQUE', 'piece'),
    (current_setting('app.current_tenant', true), 'AUTO_PARTS',  'Phụ tùng',                 'Phụ tùng thay thế: nhớt, ắc-quy, lốp, xích...', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE', 'Phụ kiện',                 'Phụ kiện: nón bảo hiểm, đồ chơi xe, áo mưa', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'SERVICE',     'Công sửa chữa / Bảo dưỡng','Tiền công sửa chữa, bảo dưỡng, lắp đặt', 'NO_INVENTORY', 'session')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories ────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), v.name, NULL
FROM (VALUES
    ('Xe máy'), ('Xe điện'), ('Xe đạp'), ('Phụ tùng'), ('Phụ kiện'), ('Dịch vụ sửa chữa')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c
    WHERE c.tenant_id = current_setting('app.current_tenant', true)
      AND c.name = v.name AND c.parent_id IS NULL
);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Xe máy số',        'Xe máy'),
    ('Xe tay ga',        'Xe máy'),
    ('Xe côn tay',       'Xe máy'),
    ('Xe đạp điện',      'Xe điện'),
    ('Xe máy điện',      'Xe điện'),
    ('Xe đạp thường',    'Xe đạp'),
    ('Xe đạp thể thao',  'Xe đạp'),
    ('Nhớt & Dầu',       'Phụ tùng'),
    ('Ắc-quy & Bình điện','Phụ tùng'),
    ('Lốp & Săm',        'Phụ tùng'),
    ('Nón bảo hiểm',     'Phụ kiện'),
    ('Đồ chơi xe',       'Phụ kiện'),
    ('Bảo dưỡng định kỳ','Dịch vụ sửa chữa'),
    ('Sửa chữa',         'Dịch vụ sửa chữa')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category ex
    WHERE ex.tenant_id = current_setting('app.current_tenant', true)
      AND ex.name = c.name AND ex.parent_id = p.id
);

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Đại lý xe máy chính hãng',     'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp phụ tùng / phụ kiện','VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5a. Sample vehicles (MOTORBIKE / E_BIKE / BICYCLE — UNIQUE, price is per-unit) ──
-- Vehicles are UNIQUE-mode: each demo product represents one chiếc. The per-unit số khung/số
-- máy live on the vehicle_unit registry; these rows just give the owner a starting catalog.
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('XE-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, 'piece',
    pt.id, 'ACTIVE'
FROM (VALUES
    (1, 'MOTORBIKE', 'Honda Wave Alpha 110', 'Xe số phổ thông 110cc, mới',        18500000, 17200000),
    (2, 'MOTORBIKE', 'Honda Vision 2024',    'Xe tay ga 110cc, mới',              33500000, 31000000),
    (3, 'MOTORBIKE', 'Yamaha Sirius',        'Xe số 110cc, mới',                  21500000, 20000000)
) AS seq(n, type_code, item_name, item_desc, item_price, item_cost)
JOIN product_type pt ON pt.code = seq.type_code
    AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('XE-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, 'piece',
    pt.id, 'ACTIVE'
FROM (VALUES
    (11, 'E_BIKE',  'Xe đạp điện VinFast Klara', 'Xe đạp điện pin lithium',       13900000, 12500000),
    (12, 'E_BIKE',  'Xe máy điện Yadea',         'Xe máy điện 1200W',             16900000, 15200000),
    (21, 'BICYCLE', 'Xe đạp thể thao Asama',     'Xe đạp địa hình khung nhôm',     3200000,  2600000),
    (22, 'BICYCLE', 'Xe đạp trẻ em 16 inch',     'Xe đạp cho bé 4-7 tuổi',         1500000,  1100000)
) AS seq(n, type_code, item_name, item_desc, item_price, item_cost)
JOIN product_type pt ON pt.code = seq.type_code
    AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5b. Sample parts & accessories (AUTO_PARTS / CONVENIENCE — TRACKED) ──
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('XE-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, 'piece',
    pt.id, 'ACTIVE'
FROM (VALUES
    (101, 'AUTO_PARTS',  'Nhớt xe máy Honda 0.8L', 'Dầu nhớt động cơ chính hãng', 90000,  60000),
    (102, 'AUTO_PARTS',  'Bình ắc-quy 12V',        'Ắc-quy khô cho xe máy',       320000, 240000),
    (103, 'AUTO_PARTS',  'Lốp sau 80/90-17',       'Lốp xe máy không săm',        420000, 300000),
    (104, 'AUTO_PARTS',  'Xích tải IRC',           'Xích tải xe số',              180000, 120000),
    (111, 'CONVENIENCE', 'Nón bảo hiểm 3/4 đầu',   'Nón bảo hiểm đạt chuẩn',      250000, 150000),
    (112, 'CONVENIENCE', 'Áo mưa bộ',              'Áo mưa 2 lớp',                120000, 70000)
) AS seq(n, type_code, item_name, item_desc, item_price, item_cost)
JOIN product_type pt ON pt.code = seq.type_code
    AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5c. Sample services — repair / maintenance labor (SERVICE) ──
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('XE-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, 0, 'session',
    pt.id, 'ACTIVE'
FROM (VALUES
    (201, 'Công bảo dưỡng định kỳ',  'Thay nhớt, kiểm tra tổng quát',  120000),
    (202, 'Công thay nhớt',          'Tiền công thay nhớt',            30000),
    (203, 'Công sửa chữa cơ bản',    'Tiền công sửa chữa thông thường',80000),
    (204, 'Công kiểm tra / chẩn đoán','Phí kiểm tra và chẩn đoán lỗi',  50000)
) AS seq(n, item_name, item_desc, item_price)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'SERVICE' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 7. Inventory for TRACKED parts/accessories (vehicles are UNIQUE, auto-managed; services skip) ──
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 20, 5, 20, p.cost_price, 'Kho cửa hàng', FALSE
FROM product p
JOIN product_type pt ON pt.id = p.product_type_id
WHERE p.sku LIKE 'XE-DEMO-%'
  AND pt.code IN ('AUTO_PARTS', 'CONVENIENCE')
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) WHERE variant_id IS NULL AND deleted = false DO NOTHING;

-- ── 8. Loyalty program ────────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 9. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',          1),
    (current_setting('app.current_tenant', true), 'Bạc',       20000000,  1.25, '#9E9E9E', 'Chi tiêu từ 20 triệu VND',   2),
    (current_setting('app.current_tenant', true), 'Vàng',      50000000,  1.50, '#FFC107', 'Chi tiêu từ 50 triệu VND',   3),
    (current_setting('app.current_tenant', true), 'Kim cương', 200000000, 2.00, '#00BCD4', 'Chi tiêu từ 200 triệu VND',  4);

-- ── 10. Product stamp template (vehicles get a label with SKU + price) ──
-- NOTE: the default POS_RECEIPT ("Phiếu giao xe") is seeded by
-- TenantSeedService.seedShopTypeTemplates() in Java — do not duplicate it here.
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem dán xe / phụ tùng', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": true,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": false,
  "labelWidth": 40,
  "labelHeight": 30
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups + definitions — MOTORBIKE ────────────
-- Group shape copies PawnVehicleEntity (hãng/model/đời/số khung/số máy/biển số/màu/tình trạng)
-- plus giấy tờ + số km. Four groups: thông tin xe · định danh · tình trạng · giấy tờ.
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, g.code, g.name, g.ord
FROM product_type, (VALUES
    ('vehicle_info', 'Thông tin xe', 1),
    ('identity',     'Định danh',    2),
    ('condition',    'Tình trạng',   3),
    ('paperwork',    'Giấy tờ',      4)
) AS g(code, name, ord)
WHERE product_type.code = 'MOTORBIKE' AND product_type.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, d.code, d.name, d.data_type, d.req, d.sch, d.flt, d.ord
FROM (VALUES
    ('vehicle_info', 'brand',           'Hãng xe',                 'STRING',  TRUE,  TRUE,  TRUE,  1),
    ('vehicle_info', 'model',           'Dòng xe / Model',         'STRING',  TRUE,  TRUE,  TRUE,  2),
    ('vehicle_info', 'year',            'Đời xe / Năm SX',         'NUMBER',  FALSE, FALSE, TRUE,  3),
    ('vehicle_info', 'engine_cc',       'Dung tích (cc)',          'NUMBER',  FALSE, FALSE, TRUE,  4),
    ('vehicle_info', 'color',           'Màu sắc',                 'STRING',  FALSE, FALSE, TRUE,  5),
    ('identity',     'chassis_number',  'Số khung',                'STRING',  TRUE,  TRUE,  FALSE, 1),
    ('identity',     'engine_number',   'Số máy',                  'STRING',  TRUE,  TRUE,  FALSE, 2),
    ('identity',     'license_plate',   'Biển số (nếu có)',        'STRING',  FALSE, TRUE,  FALSE, 3),
    ('condition',    'condition_grade', 'Tình trạng (Mới/Cũ)',     'STRING',  TRUE,  FALSE, TRUE,  1),
    ('condition',    'odometer_km',     'Số km đã đi',             'NUMBER',  FALSE, FALSE, TRUE,  2),
    ('condition',    'notes',           'Ghi chú tình trạng',      'TEXT',    FALSE, FALSE, FALSE, 3),
    ('paperwork',    'registration_doc','Cà-vẹt / Đăng ký xe',     'STRING',  FALSE, TRUE,  FALSE, 1),
    ('paperwork',    'paperwork_status','Hồ sơ gốc (Đủ/Thiếu/Đang sang tên)', 'STRING', FALSE, FALSE, TRUE, 2)
) AS d(grp, code, name, data_type, req, sch, flt, ord)
JOIN product_type pt ON pt.code = 'MOTORBIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = d.grp
    AND ag.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11b. Attribute groups + definitions — E_BIKE (công suất W thay cho cc) ──
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, g.code, g.name, g.ord
FROM product_type, (VALUES
    ('vehicle_info', 'Thông tin xe', 1),
    ('identity',     'Định danh',    2),
    ('condition',    'Tình trạng',   3),
    ('paperwork',    'Giấy tờ',      4)
) AS g(code, name, ord)
WHERE product_type.code = 'E_BIKE' AND product_type.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, d.code, d.name, d.data_type, d.req, d.sch, d.flt, d.ord
FROM (VALUES
    ('vehicle_info', 'brand',           'Hãng xe',                 'STRING',  TRUE,  TRUE,  TRUE,  1),
    ('vehicle_info', 'model',           'Dòng xe / Model',         'STRING',  TRUE,  TRUE,  TRUE,  2),
    ('vehicle_info', 'year',            'Đời xe / Năm SX',         'NUMBER',  FALSE, FALSE, TRUE,  3),
    ('vehicle_info', 'motor_power_w',   'Công suất (W)',           'NUMBER',  FALSE, FALSE, TRUE,  4),
    ('vehicle_info', 'battery_type',    'Loại pin / ắc-quy',       'STRING',  FALSE, FALSE, TRUE,  5),
    ('vehicle_info', 'color',           'Màu sắc',                 'STRING',  FALSE, FALSE, TRUE,  6),
    ('identity',     'chassis_number',  'Số khung',                'STRING',  TRUE,  TRUE,  FALSE, 1),
    ('identity',     'motor_number',    'Số động cơ (nếu có)',     'STRING',  FALSE, TRUE,  FALSE, 2),
    ('condition',    'condition_grade', 'Tình trạng (Mới/Cũ)',     'STRING',  TRUE,  FALSE, TRUE,  1),
    ('condition',    'odometer_km',     'Số km đã đi',             'NUMBER',  FALSE, FALSE, TRUE,  2),
    ('paperwork',    'paperwork_status','Giấy tờ (Đủ/Thiếu)',      'STRING',  FALSE, FALSE, TRUE,  1)
) AS d(grp, code, name, data_type, req, sch, flt, ord)
JOIN product_type pt ON pt.code = 'E_BIKE' AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = d.grp
    AND ag.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11c. Attribute groups + definitions — BICYCLE (no engine) ──
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, g.code, g.name, g.ord
FROM product_type, (VALUES
    ('vehicle_info', 'Thông tin xe', 1),
    ('identity',     'Định danh',    2),
    ('condition',    'Tình trạng',   3)
) AS g(code, name, ord)
WHERE product_type.code = 'BICYCLE' AND product_type.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, d.code, d.name, d.data_type, d.req, d.sch, d.flt, d.ord
FROM (VALUES
    ('vehicle_info', 'brand',           'Hãng xe',                 'STRING',  TRUE,  TRUE,  TRUE,  1),
    ('vehicle_info', 'model',           'Dòng xe / Model',         'STRING',  FALSE, TRUE,  TRUE,  2),
    ('vehicle_info', 'frame_size',      'Cỡ khung / Kích thước',   'STRING',  FALSE, FALSE, TRUE,  3),
    ('vehicle_info', 'color',           'Màu sắc',                 'STRING',  FALSE, FALSE, TRUE,  4),
    ('identity',     'chassis_number',  'Số khung (nếu có)',       'STRING',  FALSE, TRUE,  FALSE, 1),
    ('condition',    'condition_grade', 'Tình trạng (Mới/Cũ)',     'STRING',  TRUE,  FALSE, TRUE,  1)
) AS d(grp, code, name, data_type, req, sch, flt, ord)
JOIN product_type pt ON pt.code = 'BICYCLE' AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = d.grp
    AND ag.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11d. Attribute groups + definitions — AUTO_PARTS ──────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'part_info', 'Thông tin phụ tùng', 1
FROM product_type WHERE code = 'AUTO_PARTS' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, d.code, d.name, d.data_type, d.req, d.sch, d.flt, d.ord
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'part_info'
    AND ag.tenant_id = current_setting('app.current_tenant', true)
CROSS JOIN (VALUES
    ('brand',          'Hãng / Thương hiệu',  'STRING', FALSE, TRUE,  TRUE,  1),
    ('compatible_with','Dùng cho xe',         'STRING', FALSE, TRUE,  TRUE,  2),
    ('part_code',      'Mã phụ tùng',         'STRING', FALSE, TRUE,  FALSE, 3)
) AS d(code, name, data_type, req, sch, flt, ord)
WHERE pt.code = 'AUTO_PARTS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
