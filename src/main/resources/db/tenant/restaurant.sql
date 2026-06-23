-- ============================================================
-- TENANT SEED — RESTAURANT / QUÁN ĂN
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types ─────────────────────────────────────────
INSERT INTO product_type (tenant_id, code, name, description, default_inventory_mode, default_unit) VALUES
    (current_setting('app.current_tenant', true), 'FOOD',         'Thực phẩm',                  'Thực phẩm và đồ ăn', 'TRACKED', 'piece'),
    (current_setting('app.current_tenant', true), 'BEVERAGE',     'Đồ uống',                    'Nước giải khát, bia, nước suối', 'TRACKED', 'bottle'),
    (current_setting('app.current_tenant', true), 'DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược', 'TRACKED', 'box'),
    (current_setting('app.current_tenant', true), 'CONVENIENCE',  'Hàng tiêu dùng',             'Hàng tiêu dùng thiết yếu', 'TRACKED', 'piece'),
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

-- ── 2. Categories ─────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Món chính',       NULL),
    (current_setting('app.current_tenant', true), 'Khai vị',         NULL),
    (current_setting('app.current_tenant', true), 'Tráng miệng',     NULL),
    (current_setting('app.current_tenant', true), 'Đồ uống',         NULL),
    (current_setting('app.current_tenant', true), 'Món đặc biệt',    NULL),
    (current_setting('app.current_tenant', true), 'Cơm & Bún & Phở', NULL),
    (current_setting('app.current_tenant', true), 'Lẩu',             NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Cơm chiên',           'Cơm & Bún & Phở'),
    ('Phở & Bún bò',        'Cơm & Bún & Phở'),
    ('Mì & Hủ tiếu',        'Cơm & Bún & Phở'),
    ('Cơm phần',            'Cơm & Bún & Phở'),
    ('Món xào',             'Món chính'),
    ('Món nướng',           'Món chính'),
    ('Món hấp',             'Món chính'),
    ('Canh & Súp',          'Khai vị'),
    ('Gỏi & Salad',         'Khai vị'),
    ('Nước ngọt',           'Đồ uống'),
    ('Nước suối',           'Đồ uống'),
    ('Trà & Cà phê',        'Đồ uống'),
    ('Bia & Rượu',          'Đồ uống')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp nguyên liệu nhà hàng', 'VND-001', NULL, NULL, 'NET_15', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp đồ uống & bia',        'VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5. Sample products ────────────────────────────────────────
-- Starter menu so the restaurant has something sellable on day one (per the seed
-- convention every shop type ships sample items). FOOD + BEVERAGE; no inventory rows
-- (F&B menu items are not stock-tracked).
INSERT INTO product
    (tenant_id, sku, name, description, price, cost_price, unit, product_type_id, status)
SELECT
    current_setting('app.current_tenant', true),
    CONCAT('RES-DEMO-', LPAD(seq.n::text, 3, '0')),
    seq.item_name, seq.item_desc, seq.item_price, seq.item_cost, seq.item_unit,
    pt.id, 'ACTIVE'
FROM (VALUES
    (1,  'Phở bò tái',            'Phở bò tái nước dùng truyền thống',  55000,  33000, 'bowl',    'FOOD'),
    (2,  'Cơm chiên Dương Châu',  'Cơm chiên thập cẩm',                 50000,  30000, 'plate',   'FOOD'),
    (3,  'Bún chả Hà Nội',        'Bún chả thịt nướng',                 50000,  30000, 'bowl',    'FOOD'),
    (4,  'Gỏi cuốn tôm thịt',     'Gỏi cuốn tươi (2 cuốn)',             35000,  20000, 'portion', 'FOOD'),
    (5,  'Chả giò rế',            'Chả giò chiên giòn (4 cuốn)',        40000,  24000, 'portion', 'FOOD'),
    (6,  'Rau muống xào tỏi',     'Rau muống xào tỏi',                  45000,  22000, 'plate',   'FOOD'),
    (7,  'Cá kho tộ',             'Cá basa kho tộ',                     75000,  45000, 'plate',   'FOOD'),
    (8,  'Gà nướng sả',           'Gà nướng sả ớt',                    120000,  72000, 'plate',   'FOOD'),
    (9,  'Lẩu thái hải sản',      'Lẩu thái chua cay hải sản',         250000, 150000, 'pot',     'FOOD'),
    (10, 'Chè ba màu',            'Chè ba màu mát lạnh',                20000,  10000, 'cup',     'FOOD'),
    (11, 'Trà đá',                'Trà đá',                              5000,   1000, 'glass',   'BEVERAGE'),
    (12, 'Bia Sài Gòn lon',       'Bia Sài Gòn lon 330ml',              20000,  13000, 'can',     'BEVERAGE')
) AS seq(n, item_name, item_desc, item_price, item_cost, item_unit, type_code)
JOIN product_type pt
    ON pt.code = seq.type_code
   AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 6. Product → category links ───────────────────────────────
INSERT INTO product_category (tenant_id, product_id, category_id)
SELECT current_setting('app.current_tenant', true), p.id, c.id
FROM (VALUES
    ('RES-DEMO-001', 'Cơm & Bún & Phở'),
    ('RES-DEMO-002', 'Cơm & Bún & Phở'),
    ('RES-DEMO-003', 'Cơm & Bún & Phở'),
    ('RES-DEMO-004', 'Khai vị'),
    ('RES-DEMO-005', 'Khai vị'),
    ('RES-DEMO-006', 'Món chính'),
    ('RES-DEMO-007', 'Món chính'),
    ('RES-DEMO-008', 'Món đặc biệt'),
    ('RES-DEMO-009', 'Lẩu'),
    ('RES-DEMO-010', 'Tráng miệng'),
    ('RES-DEMO-011', 'Đồ uống'),
    ('RES-DEMO-012', 'Đồ uống')
) AS m(sku, cat)
JOIN product p ON p.sku = m.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = m.cat AND c.parent_id IS NULL
    AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id, category_id) DO NOTHING;

-- ── 6b. Dining tables ─────────────────────────────────────────
-- Starter tables so a restaurant can take dine-in / per-table QR orders immediately.
-- qr_token is random per the schema (URLs are not enumerable); idempotent via NOT EXISTS
-- on table_number (no natural unique key for ON CONFLICT).
INSERT INTO shop_table (tenant_id, table_number, capacity, status, display_order, qr_token)
SELECT current_setting('app.current_tenant', true), v.table_number, v.capacity,
       'AVAILABLE', v.display_order, gen_random_uuid()::text
FROM (VALUES
    ('1', 4, 1),
    ('2', 4, 2),
    ('3', 6, 3),
    ('4', 2, 4)
) AS v(table_number, capacity, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM shop_table st
    WHERE st.tenant_id = current_setting('app.current_tenant', true)
      AND st.table_number = v.table_number
);

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
    (current_setting('app.current_tenant', true), 'Thành viên',  0,        1.00, '#CD7F32', 'Thành viên cơ bản',         1),
    (current_setting('app.current_tenant', true), 'Bạc',         1000000,  1.25, '#9E9E9E', 'Chi tiêu từ 1 triệu VND',   2),
    (current_setting('app.current_tenant', true), 'Vàng',        5000000,  1.50, '#FFC107', 'Chi tiêu từ 5 triệu VND',   3),
    (current_setting('app.current_tenant', true), 'VIP',         20000000, 2.00, '#00BCD4', 'Chi tiêu từ 20 triệu VND',  4);

-- ── 9. Print templates ────────────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Mặc định', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nChúc ngon miệng!",
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
  "showExpiry": false,
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
  "showExpiry": false,
  "labelWidth": 60,
  "labelHeight": 38
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 10. Attribute groups & definitions (FOOD) ─────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'dish_info', 'Thông tin món ăn', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'dietary', 'Chế độ ăn & Dị ứng', 2
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'dish_type', 'Loại món', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'spice_level', 'Độ cay', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'prep_time_min', 'Thời gian chế biến (phút)', 'NUMBER', FALSE, FALSE, FALSE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'allergens', 'Chất gây dị ứng', 'TEXT', FALSE, FALSE, FALSE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dietary'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'is_vegetarian', 'Món chay', 'BOOLEAN', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dietary'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute groups & definitions (BEVERAGE)
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'bev_info', 'Thông tin đồ uống', 1
FROM product_type WHERE code = 'BEVERAGE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'alcohol_pct', 'Độ cồn (%)', 'NUMBER', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_info'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'volume_ml', 'Dung tích (ml)', 'NUMBER', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_info'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'serving_temp', 'Nhiệt độ phục vụ', 'STRING', FALSE, FALSE, TRUE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_info'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;

-- ── 12. Modifier groups ───────────────────────────────────────
-- Starter per-dish modifiers so a restaurant has ready-made groups to attach immediately
-- (mirrors coffee_shop.sql). Idempotent via NOT EXISTS (no natural unique key for ON CONFLICT).
INSERT INTO modifier_groups (tenant_id, name, min_select, max_select, required, sort_order)
SELECT current_setting('app.current_tenant', true), v.name, v.min_select, v.max_select, v.required, v.sort_order
FROM (VALUES
    ('Mức cay',   1, 1, TRUE,  1),
    ('Tùy chọn',  0, 5, FALSE, 2),
    ('Thêm món',  0, 5, FALSE, 3),
    ('Đóng gói',  1, 1, TRUE,  4)
) AS v(name, min_select, max_select, required, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM modifier_groups g
    WHERE g.tenant_id = current_setting('app.current_tenant', true) AND g.name = v.name
);

INSERT INTO modifier_options (tenant_id, modifier_group_id, name, price_delta, sort_order)
SELECT current_setting('app.current_tenant', true), g.id, o.name, o.price_delta, o.sort_order
FROM modifier_groups g
JOIN (VALUES
    ('Mức cay',   'Không cay',        0,     1),
    ('Mức cay',   'Cay nhẹ',          0,     2),
    ('Mức cay',   'Cay vừa',          0,     3),
    ('Mức cay',   'Cay nhiều',        0,     4),
    ('Tùy chọn',  'Thêm hành',        0,     1),
    ('Tùy chọn',  'Không hành',       0,     2),
    ('Tùy chọn',  'Không rau',        0,     3),
    ('Tùy chọn',  'Không mỡ',         0,     4),
    ('Thêm món',  'Thêm trứng',       5000,  1),
    ('Thêm món',  'Thêm thịt',        15000, 2),
    ('Thêm món',  'Thêm rau',         5000,  3),
    ('Thêm món',  'Cơm thêm',         5000,  4),
    ('Đóng gói',  'Ăn tại chỗ',       0,     1),
    ('Đóng gói',  'Mang đi',          0,     2)
) AS o(group_name, name, price_delta, sort_order) ON o.group_name = g.name
WHERE g.tenant_id = current_setting('app.current_tenant', true)
  AND NOT EXISTS (
    SELECT 1 FROM modifier_options mo
    WHERE mo.tenant_id = current_setting('app.current_tenant', true)
      AND mo.modifier_group_id = g.id AND mo.name = o.name
  );

-- ── 13. Attach a starter modifier group to a dish ─────────────
-- Give one sample dish a required modifier group so customers see options on the QR
-- menu out of the box (and so the QR-ordering modifier flow is exercisable).
INSERT INTO product_modifier_groups (tenant_id, product_id, modifier_group_id, sort_order)
SELECT current_setting('app.current_tenant', true), p.id, g.id, 0
FROM product p
JOIN modifier_groups g
    ON g.tenant_id = current_setting('app.current_tenant', true) AND g.name = 'Mức cay'
WHERE p.tenant_id = current_setting('app.current_tenant', true) AND p.sku = 'RES-DEMO-001'
  AND NOT EXISTS (
    SELECT 1 FROM product_modifier_groups pmg
    WHERE pmg.tenant_id = current_setting('app.current_tenant', true)
      AND pmg.product_id = p.id AND pmg.modifier_group_id = g.id
  );
