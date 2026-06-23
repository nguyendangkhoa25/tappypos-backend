-- ============================================================
-- TENANT SEED — BAKERY / TIỆM BÁNH
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING/DO UPDATE or WHERE NOT EXISTS).
-- tenant_id sourced from app.current_tenant session variable.
--
-- Dominant product type: BAKERY (bánh đặt / bánh kem) — carries cake-specific
-- attributes (flavor, size, tiers, message, lead time). FOOD covers ready-on-shelf
-- bánh mì / bánh ngọt. The default POS_RECEIPT ("Mặc định") is seeded here because
-- BAKERY is plain takeaway retail and TenantSeedService.seedShopTypeTemplates()
-- does NOT seed a type-specific template for it.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'BAKERY',       'Bánh đặt / Bánh kem',        'Bánh kem sinh nhật và bánh đặt theo yêu cầu', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FOOD',         'Bánh & Thực phẩm',           'Bánh mì, bánh ngọt và thực phẩm bán tại quầy', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước giải khát, cà phê, nước suối', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Hàng tiêu dùng',             'Hàng tiêu dùng thiết yếu', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược', 'TRACKED', 'box'),
    (current_setting('app.current_tenant', true), 'BIKE',         'Xe đạp / Xe máy',            'Xe đạp và phụ tùng xe máy', 'UNIQUE', 'piece'),
    (current_setting('app.current_tenant', true), 'HARDWARE',     'Đồ sắt / Dụng cụ',          'Đồ sắt và dụng cụ', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'CLOTHING',     'Quần áo / May mặc',          'Quần áo và phụ kiện', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'ELECTRONICS',  'Điện tử',                    'Thiết bị điện tử', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'FURNITURE',    'Đồ nội thất',                'Nội thất gia đình', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEAUTY',       'Làm đẹp / Chăm sóc cá nhân','Sản phẩm làm đẹp và vệ sinh cá nhân', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'TOYS',         'Đồ chơi / Trò chơi',        'Đồ chơi và trò chơi', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BOOKS',        'Sách / Văn phòng phẩm',     'Sách và văn phòng phẩm', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'SPORTS',       'Thể thao / Ngoài trời',     'Thiết bị thể thao', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'AUTO_PARTS',   'Phụ tùng ô tô',             'Phụ tùng và phụ kiện ô tô', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'APPLIANCES',   'Đồ gia dụng',                'Thiết bị gia dụng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'OFFICE',       'Văn phòng phẩm',             'Đồ dùng văn phòng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'PET',          'Thú cưng',                  'Thức ăn và phụ kiện thú cưng', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'HEALTH',       'Sức khỏe / Dinh dưỡng',     'Sản phẩm sức khỏe và dinh dưỡng', 'TRACKED', 'piece')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, default_inventory_mode = EXCLUDED.default_inventory_mode;

-- ── 2. Categories (bakery taxonomy) ───────────────────────────
-- category has no natural unique key, so guard each insert with WHERE NOT EXISTS.
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), v.name, NULL
FROM (VALUES
    ('Bánh kem & Bánh đặt'), ('Bánh mì'), ('Bánh ngọt'),
    ('Bánh trung thu & Mùa vụ'), ('Bánh quy & Đồ khô'), ('Đồ uống')
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
    ('Bánh kem sinh nhật',  'Bánh kem & Bánh đặt'),
    ('Bánh kem theo yêu cầu','Bánh kem & Bánh đặt'),
    ('Cupcake & Bánh nhỏ',  'Bánh kem & Bánh đặt'),
    ('Bánh mì mặn',         'Bánh mì'),
    ('Bánh mì ngọt',        'Bánh mì'),
    ('Bánh bông lan',       'Bánh ngọt'),
    ('Bánh su & Tiramisu',  'Bánh ngọt'),
    ('Bánh croissant & Pastry','Bánh ngọt'),
    ('Nước suối & Nước ngọt','Đồ uống'),
    ('Cà phê & Trà',        'Đồ uống')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category ex
    WHERE ex.tenant_id = current_setting('app.current_tenant', true)
      AND ex.name = c.name AND ex.parent_id = p.id
);

-- ── 3. Vendors (ingredient & packaging suppliers) ─────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp nguyên liệu làm bánh', 'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp bao bì & hộp bánh',    'VND-002', NULL, NULL, 'NET_15', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5a. Sample products — ready-on-shelf (FOOD / BEVERAGE) ────
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('BAKERY-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1,  'Bánh mì thịt',            'Bánh mì kẹp thịt nguội, chả lụa, rau dưa',   20000,   9000,  'piece'),
    (2,  'Bánh mì pate',            'Bánh mì pate gan truyền thống',              18000,   8000,  'piece'),
    (3,  'Bánh mì ngọt',            'Bánh mì ngọt nhân bơ sữa',                   12000,   5000,  'piece'),
    (4,  'Bánh croissant bơ',       'Bánh sừng bò bơ Pháp',                       25000,  12000,  'piece'),
    (5,  'Bánh su kem',             'Bánh su nhân kem trứng',                     15000,   6000,  'piece'),
    (6,  'Bánh bông lan trứng muối','Bánh bông lan cuộn trứng muối chà bông',     35000,  18000,  'piece'),
    (7,  'Bánh tiramisu',           'Bánh tiramisu cà phê (phần)',                40000,  20000,  'piece'),
    (8,  'Cupcake kem',             'Bánh cupcake phủ kem tươi',                  22000,  10000,  'piece'),
    (9,  'Bánh quy bơ (hộp)',       'Hộp bánh quy bơ thập cẩm',                   85000,  45000,  'box'),
    (10, 'Bánh trung thu thập cẩm', 'Bánh trung thu nhân thập cẩm (mùa vụ)',      65000,  35000,  'piece'),
    (11, 'Nước suối Lavie 500ml',   'Nước suối đóng chai 500ml',                  10000,   5000,  'bottle'),
    (12, 'Cà phê sữa đá lon',       'Cà phê sữa đóng lon',                        15000,   8000,  'can')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5b. Sample products — custom cakes (BAKERY product type) ──
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('BAKERY-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, 'piece',
    pt.id, 'ACTIVE'
FROM (VALUES
    (101, 'Bánh kem sinh nhật 16cm',  'Bánh kem sinh nhật cỡ nhỏ, đường kính 16cm',  180000,  90000),
    (102, 'Bánh kem sinh nhật 20cm',  'Bánh kem sinh nhật cỡ vừa, đường kính 20cm',  280000, 140000),
    (103, 'Bánh kem sinh nhật 24cm',  'Bánh kem sinh nhật cỡ lớn, đường kính 24cm',  380000, 190000),
    (104, 'Bánh kem 2 tầng',          'Bánh kem 2 tầng đặt theo yêu cầu',            850000, 450000),
    (105, 'Bánh kem theo mẫu',        'Bánh kem trang trí theo mẫu khách đặt',       500000, 260000),
    (106, 'Bánh cưới mini',           'Bánh cưới cỡ nhỏ trang trí theo yêu cầu',    1200000, 650000)
) AS seq(n, item_name, item_desc, item_price, item_cost)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'BAKERY' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 6. Product → category links ──────────────────────────────
-- Assign each sample product to a parent category so the POS category tabs and
-- category filtering work. Ingredients (BAKERY-INGR-*) are intentionally left uncategorised.
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('BAKERY-DEMO-001', 'Bánh mì'),
    ('BAKERY-DEMO-002', 'Bánh mì'),
    ('BAKERY-DEMO-003', 'Bánh mì'),
    ('BAKERY-DEMO-004', 'Bánh ngọt'),
    ('BAKERY-DEMO-005', 'Bánh ngọt'),
    ('BAKERY-DEMO-006', 'Bánh ngọt'),
    ('BAKERY-DEMO-007', 'Bánh ngọt'),
    ('BAKERY-DEMO-008', 'Bánh ngọt'),
    ('BAKERY-DEMO-009', 'Bánh quy & Đồ khô'),
    ('BAKERY-DEMO-010', 'Bánh trung thu & Mùa vụ'),
    ('BAKERY-DEMO-011', 'Đồ uống'),
    ('BAKERY-DEMO-012', 'Đồ uống'),
    ('BAKERY-DEMO-101', 'Bánh kem & Bánh đặt'),
    ('BAKERY-DEMO-102', 'Bánh kem & Bánh đặt'),
    ('BAKERY-DEMO-103', 'Bánh kem & Bánh đặt'),
    ('BAKERY-DEMO-104', 'Bánh kem & Bánh đặt'),
    ('BAKERY-DEMO-105', 'Bánh kem & Bánh đặt'),
    ('BAKERY-DEMO-106', 'Bánh kem & Bánh đặt')
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
    p.tenant_id, p.id, 10, 5, 20, p.cost_price, 'Quầy bánh', FALSE
FROM product p
WHERE p.sku LIKE 'BAKERY-DEMO-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
-- Match the partial unique index uq_inv_product_no_variant (V001 dropped the plain UNIQUE(product_id)).
ON CONFLICT (product_id) WHERE variant_id IS NULL AND deleted = false DO NOTHING;

-- ── 8. Loyalty program ────────────────────────────────────────
-- No unique constraint on (tenant_id); guard with WHERE NOT EXISTS so a re-run is idempotent.
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
SELECT current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM loyalty_programs lp
    WHERE lp.tenant_id = current_setting('app.current_tenant', true)
);

-- ── 9. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Thành viên', 0,        1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',        500000,   1.25, '#9E9E9E', 'Chi tiêu từ 500K VND',     2),
    (current_setting('app.current_tenant', true), 'Vàng',       2000000,  1.50, '#FFC107', 'Chi tiêu từ 2 triệu VND',  3),
    (current_setting('app.current_tenant', true), 'Kim cương',  10000000, 2.00, '#00BCD4', 'Chi tiêu từ 10 triệu VND', 4);

-- ── 10. Default print templates ───────────────────────────────
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
}', TRUE),
    (current_setting('app.current_tenant', true), 'PRODUCT_STAMP', 'Tem sản phẩm', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": true,
  "showBarcode": false,
  "showLocation": false,
  "showBatch": false,
  "showExpiry": true,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE),
    (current_setting('app.current_tenant', true), 'INVENTORY_STAMP', 'Tem kho', '{
  "showShopName": true,
  "showSku": true,
  "showPrice": false,
  "showBarcode": false,
  "showLocation": true,
  "showBatch": false,
  "showExpiry": true,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups (BAKERY product type) ────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'cake_info', 'Thông tin bánh', 1
FROM product_type WHERE code = 'BAKERY' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'custom_order', 'Đặt bánh theo yêu cầu', 2
FROM product_type WHERE code = 'BAKERY' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'nutrition_allergy', 'Dinh dưỡng & Dị ứng', 3
FROM product_type WHERE code = 'BAKERY' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

-- ── 11b. Attribute definitions (BAKERY product type) ──────────
-- Group: cake_info
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'flavor', 'Hương vị (socola/vani/dâu…)', 'STRING', TRUE, TRUE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cake_info'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'size', 'Kích cỡ (đường kính)', 'STRING', TRUE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cake_info'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'weight_kg', 'Trọng lượng (kg)', 'NUMBER', FALSE, FALSE, TRUE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cake_info'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'shelf_life_days', 'Hạn dùng (ngày)', 'NUMBER', FALSE, FALSE, TRUE, 4
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cake_info'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'storage_condition', 'Bảo quản', 'STRING', FALSE, FALSE, TRUE, 5
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'cake_info'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Group: custom_order
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'tiers', 'Số tầng', 'NUMBER', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'custom_order'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'cake_message', 'Chữ viết trên bánh', 'STRING', FALSE, FALSE, FALSE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'custom_order'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'lead_time_hours', 'Thời gian đặt trước (giờ)', 'NUMBER', FALSE, FALSE, TRUE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'custom_order'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Group: nutrition_allergy
INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'is_eggless', 'Không trứng', 'BOOLEAN', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'nutrition_allergy'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'allergens', 'Thành phần gây dị ứng', 'TEXT', FALSE, FALSE, FALSE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'nutrition_allergy'
WHERE pt.code = 'BAKERY' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11c. Attribute groups & definitions (FOOD product type) ───
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'food_info', 'Thông tin sản phẩm', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'food_storage', 'Bảo quản', 2
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Thương hiệu', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'package_size', 'Quy cách / Trọng lượng', 'STRING', FALSE, TRUE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'ingredients', 'Thành phần', 'TEXT', FALSE, TRUE, FALSE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'expiry_date', 'Hạn sử dụng', 'DATE', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_storage'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'storage_requirement', 'Điều kiện bảo quản', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_storage'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 12. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 13. Ingredients (nguyên liệu) — Phase 3 two-stage inventory ────────────────
-- Raw materials: product_kind = 'INGREDIENT' so they never appear on POS, only feed recipes.
-- price = 0 (not sold); cost_price is the per-unit buy cost used for true cost-per-cake.
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status, product_kind)
SELECT
    current_setting('app.current_tenant', true),
    v.sku, v.name, v.descr, 0, v.cost, v.unit, pt.id, 'ACTIVE', 'INGREDIENT'
FROM (VALUES
    ('BAKERY-INGR-001', 'Bột mì',    'Bột mì làm bánh',            25000,  'kg'),
    ('BAKERY-INGR-002', 'Đường',     'Đường trắng',                20000,  'kg'),
    ('BAKERY-INGR-003', 'Trứng gà',  'Trứng gà tươi',               3500,  'quả'),
    ('BAKERY-INGR-004', 'Kem tươi',  'Kem tươi (whipping cream)',  90000,  'kg'),
    ('BAKERY-INGR-005', 'Bơ lạt',    'Bơ lạt làm bánh',           120000,  'kg')
) AS v(sku, name, descr, cost, unit)
CROSS JOIN (
    SELECT id FROM product_type
    WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
    LIMIT 1
) pt
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 14. Ingredient inventory (kho nguyên liệu) ────────────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, deleted)
SELECT
    p.tenant_id, p.id, 50, 10, 50, p.cost_price, 'Kho nguyên liệu', FALSE
FROM product p
WHERE p.sku LIKE 'BAKERY-INGR-%'
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) WHERE variant_id IS NULL AND deleted = false DO NOTHING;

-- ── 15. Sample recipe (định lượng mẫu) for "Bánh kem sinh nhật 20cm" ───────────
-- Yields 1 cake; labor 20k + overhead 10k. Ingredient cost ≈ 73.5k → cost/cái ≈ 103.5k.
INSERT INTO recipe (tenant_id, finished_product_id, yield_quantity, labor_cost, overhead_cost, notes)
SELECT current_setting('app.current_tenant', true), p.id, 1, 20000, 10000,
       'Định lượng mẫu — bánh kem sinh nhật 20cm'
FROM product p
WHERE p.sku = 'BAKERY-DEMO-102'
  AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (finished_product_id, tenant_id) DO NOTHING;

INSERT INTO recipe_item (tenant_id, recipe_id, ingredient_product_id, quantity, unit)
SELECT current_setting('app.current_tenant', true), r.id, ing.id, v.qty, v.unit
FROM (VALUES
    ('BAKERY-INGR-001', 0.3, 'kg'),
    ('BAKERY-INGR-002', 0.2, 'kg'),
    ('BAKERY-INGR-003', 4,   'quả'),
    ('BAKERY-INGR-004', 0.4, 'kg'),
    ('BAKERY-INGR-005', 0.1, 'kg')
) AS v(ing_sku, qty, unit)
JOIN product ing ON ing.sku = v.ing_sku
    AND ing.tenant_id = current_setting('app.current_tenant', true)
JOIN product fp ON fp.sku = 'BAKERY-DEMO-102'
    AND fp.tenant_id = current_setting('app.current_tenant', true)
JOIN recipe r ON r.finished_product_id = fp.id
    AND r.tenant_id = current_setting('app.current_tenant', true)
    AND r.deleted = false
WHERE NOT EXISTS (
    SELECT 1 FROM recipe_item ri
    WHERE ri.recipe_id = r.id AND ri.ingredient_product_id = ing.id AND ri.deleted = false
);
