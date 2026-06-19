-- ============================================================
-- TENANT SEED — DEFAULT DATA: ELECTRONICS (ĐIỆN TỬ / ĐIỆN MÁY)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING/DO UPDATE or WHERE NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
--
-- Dominant product type: ELECTRONICS. Devices are tracked by serial/IMEI and
-- carry technical specs + warranty months (see the attribute groups below).
-- The POS_RECEIPT default ("Phiếu bảo hành") is seeded in Java by
-- TenantSeedService.seedShopTypeTemplates(); it is intentionally NOT seeded here.
-- ============================================================

-- ── 1. Product types (18 standard types) ─────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'ELECTRONICS',  'Điện tử / Điện máy',         'Điện thoại, máy tính, thiết bị điện tử và điện máy', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'APPLIANCES',   'Đồ gia dụng',                'Thiết bị gia dụng: nồi cơm, quạt, máy lọc nước', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Phụ kiện / Hàng tiêu dùng',  'Phụ kiện và hàng tiêu dùng đi kèm', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'SERVICE',      'Dịch vụ / Công sửa chữa',    'Dịch vụ kỹ thuật, công sửa chữa, lắp đặt', 'NO_INVENTORY', 'session'),
    (current_setting('app.current_tenant', true), 'BEAUTY',       'Làm đẹp / Chăm sóc cá nhân','Mỹ phẩm và phụ kiện làm đẹp', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FOOD',         'Thực phẩm',                  'Thực phẩm và đồ ăn', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước giải khát, bia, nước suối', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược', 'TRACKED', 'box'),
    (current_setting('app.current_tenant', true), 'BIKE',         'Xe đạp / Xe máy',            'Xe đạp và phụ tùng xe máy', 'UNIQUE', 'piece'),
    (current_setting('app.current_tenant', true), 'HARDWARE',     'Đồ sắt / Dụng cụ',          'Đồ sắt và dụng cụ', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CLOTHING',     'Quần áo / May mặc',          'Quần áo và phụ kiện thời trang', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FURNITURE',    'Đồ nội thất',                'Nội thất gia đình', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'TOYS',         'Đồ chơi / Trò chơi',        'Đồ chơi và trò chơi', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BOOKS',        'Sách / Văn phòng phẩm',     'Sách và văn phòng phẩm', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'AUTO_PARTS',   'Phụ tùng ô tô',             'Phụ tùng và phụ kiện ô tô', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'OFFICE',       'Văn phòng phẩm',             'Đồ dùng văn phòng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'PET',          'Thú cưng',                  'Thức ăn và phụ kiện thú cưng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'HEALTH',       'Sức khỏe / Dinh dưỡng',     'Sản phẩm sức khỏe và dinh dưỡng', 'TRACKED', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories (điện máy taxonomy) ────────────────────────
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), v.name, NULL
FROM (VALUES
    ('Điện thoại & Máy tính bảng'), ('Máy tính & Laptop'), ('Tivi & Âm thanh'),
    ('Điện máy gia dụng'), ('Phụ kiện'), ('Dịch vụ sửa chữa')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1 FROM category c
    WHERE c.tenant_id = current_setting('app.current_tenant', true)
      AND c.name = v.name AND c.parent_id IS NULL
);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Điện thoại',          'Điện thoại & Máy tính bảng'),
    ('Máy tính bảng',       'Điện thoại & Máy tính bảng'),
    ('Laptop',              'Máy tính & Laptop'),
    ('Máy tính để bàn',     'Máy tính & Laptop'),
    ('Màn hình',            'Máy tính & Laptop'),
    ('Tivi',                'Tivi & Âm thanh'),
    ('Loa & Tai nghe',      'Tivi & Âm thanh'),
    ('Tủ lạnh',             'Điện máy gia dụng'),
    ('Máy giặt',            'Điện máy gia dụng'),
    ('Máy lạnh',            'Điện máy gia dụng'),
    ('Sạc & Cáp',           'Phụ kiện'),
    ('Ốp lưng & Bao da',    'Phụ kiện'),
    ('Linh kiện thay thế',  'Phụ kiện'),
    ('Sửa điện thoại',      'Dịch vụ sửa chữa'),
    ('Sửa laptop',          'Dịch vụ sửa chữa'),
    ('Sửa điện máy',        'Dịch vụ sửa chữa')
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
    (current_setting('app.current_tenant', true), 'Nhà phân phối thiết bị điện tử',  'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp linh kiện & phụ kiện','VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5a. Sample products — devices (ELECTRONICS) ──────────────
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('ELEC-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, 'piece',
    pt.id, 'ACTIVE'
FROM (VALUES
    (1,  'Điện thoại Samsung Galaxy A15',  'Điện thoại Android màn hình 6.5"',         4990000,  4200000),
    (2,  'Điện thoại Xiaomi Redmi 13C',    'Điện thoại phổ thông 4GB/128GB',           3290000,  2700000),
    (3,  'Laptop HP 15s Core i5',          'Laptop văn phòng 8GB/512GB SSD',          14990000, 13000000),
    (4,  'Tivi Samsung 43" 4K',            'Smart Tivi 43 inch độ phân giải 4K',       8990000,  7800000),
    (5,  'Tủ lạnh Aqua 130L',              'Tủ lạnh 2 cánh 130 lít',                   4290000,  3600000),
    (6,  'Máy giặt LG 8.5kg',              'Máy giặt cửa trên 8.5kg',                  6490000,  5500000),
    (7,  'Tai nghe Bluetooth Sony',        'Tai nghe không dây chống ồn',              1290000,  900000),
    (8,  'Loa Bluetooth JBL Go',           'Loa di động chống nước',                   790000,   550000)
) AS seq(n, item_name, item_desc, item_price, item_cost)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5b. Sample products — accessories & spare parts (CONVENIENCE) ──
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('ELEC-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, 'piece',
    pt.id, 'ACTIVE'
FROM (VALUES
    (101, 'Cáp sạc USB-C 1m',          'Cáp sạc nhanh USB-C',                   59000,   25000),
    (102, 'Củ sạc nhanh 25W',          'Củ sạc nhanh chuẩn PD 25W',            149000,   80000),
    (103, 'Ốp lưng điện thoại',        'Ốp lưng silicon chống sốc',             49000,   18000),
    (104, 'Pin điện thoại thay thế',   'Pin thay thế cho điện thoại phổ thông', 250000,  120000),
    (105, 'Màn hình điện thoại (linh kiện)', 'Màn hình thay thế cho điện thoại',  650000,  350000)
) AS seq(n, item_name, item_desc, item_price, item_cost)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'CONVENIENCE' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5c. Sample services — repair labor (SERVICE) ──────────────
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('ELEC-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, 0, 'session',
    pt.id, 'ACTIVE'
FROM (VALUES
    (201, 'Công thay màn hình điện thoại', 'Tiền công thay màn hình',           150000),
    (202, 'Công thay pin',                 'Tiền công thay pin thiết bị',       100000),
    (203, 'Công vệ sinh laptop',           'Vệ sinh, tra keo tản nhiệt laptop', 200000),
    (204, 'Công kiểm tra / chẩn đoán',     'Phí kiểm tra và chẩn đoán lỗi',     50000)
) AS seq(n, item_name, item_desc, item_price)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'SERVICE' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 7. Inventory for sample stock products (devices + accessories) ──
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 10, 3, 10, p.cost_price, 'Kho cửa hàng', FALSE
FROM product p
JOIN product_type pt ON pt.id = p.product_type_id
WHERE p.sku LIKE 'ELEC-DEMO-%'
  AND pt.code <> 'SERVICE'
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
    (current_setting('app.current_tenant', true), 'Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',         1),
    (current_setting('app.current_tenant', true), 'Bạc',       5000000,   1.25, '#9E9E9E', 'Chi tiêu từ 5 triệu VND',   2),
    (current_setting('app.current_tenant', true), 'Vàng',      20000000,  1.50, '#FFC107', 'Chi tiêu từ 20 triệu VND',  3),
    (current_setting('app.current_tenant', true), 'Kim cương', 100000000, 2.00, '#00BCD4', 'Chi tiêu từ 100 triệu VND', 4);

-- ── 10. Default print templates ───────────────────────────────
-- NOTE: the default POS_RECEIPT ("Phiếu bảo hành") is seeded by
-- TenantSeedService.seedShopTypeTemplates() in Java — do not duplicate it here.
-- Electronics shops tag devices, so seed a product stamp (SKU + price + barcode).
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem dán sản phẩm', '{
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

-- ── 11. Attribute groups (ELECTRONICS type) ───────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'tech_specs', 'Thông số kỹ thuật', 1
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'warranty_origin', 'Bảo hành & Xuất xứ', 2
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'condition', 'Tình trạng', 3
FROM product_type WHERE code = 'ELECTRONICS' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

-- ── 11b. Attribute definitions (ELECTRONICS type) ─────────────
-- Group: tech_specs
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Hãng sản xuất', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'model', 'Model', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'serial_number', 'Số serial', 'STRING', FALSE, TRUE, FALSE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'cpu', 'Bộ xử lý (CPU)', 'STRING', FALSE, TRUE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'ram_gb', 'RAM (GB)', 'NUMBER', FALSE, FALSE, TRUE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'storage_gb', 'Bộ nhớ trong (GB)', 'NUMBER', FALSE, FALSE, TRUE, 6
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'screen_size', 'Kích thước màn hình', 'STRING', FALSE, FALSE, TRUE, 7
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'color', 'Màu sắc', 'STRING', FALSE, FALSE, TRUE, 8
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'tech_specs'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Group: warranty_origin
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'warranty_months', 'Bảo hành (tháng)', 'NUMBER', FALSE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'warranty_origin'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'warranty_origin'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Group: condition
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'condition_grade', 'Tình trạng (Mới/Like new/Cũ)', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'condition'
WHERE pt.code = 'ELECTRONICS' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
