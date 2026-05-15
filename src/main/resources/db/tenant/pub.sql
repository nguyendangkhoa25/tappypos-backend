-- ============================================================
-- TENANT SEED — PUB / QUÁN NHẬU (general)
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

-- ── 2. Categories ─────────────────────────────────────────────
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Bia & Rượu',    NULL),
    (current_setting('app.current_tenant', true), 'Đồ nhậu',       NULL),
    (current_setting('app.current_tenant', true), 'Nước giải khát',NULL),
    (current_setting('app.current_tenant', true), 'Món nóng',      NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Bia lon & Bia chai',   'Bia & Rượu'),
    ('Két bia',              'Bia & Rượu'),
    ('Rượu mạnh',            'Bia & Rượu'),
    ('Mồi khô',              'Đồ nhậu'),
    ('Mồi tươi & nướng',     'Đồ nhậu'),
    ('Trứng & Đặc sản',      'Đồ nhậu'),
    ('Nước ngọt',            'Nước giải khát'),
    ('Nước suối',            'Nước giải khát'),
    ('Canh & Lẩu',           'Món nóng'),
    ('Cơm & Bún',            'Món nóng')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà phân phối bia & đồ uống', 'VND-001', NULL, NULL, 'NET_15', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp thực phẩm nhậu',  'VND-002', NULL, NULL, 'NET_7',  TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 4. Products ───────────────────────────────────────────────
INSERT INTO product (tenant_id, product_type_id, sku, name, description, price, cost_price, unit, vendor_id, status)
SELECT
    current_setting('app.current_tenant', true),
    pt.id,
    p.sku, p.name, p.description, p.price::NUMERIC, p.cost_price::NUMERIC, p.unit,
    v.id,
    'ACTIVE'
FROM (VALUES
    ('BEVERAGE','VND-001','PUB-BEV-001','Bia Saigon Special lon',   'Bia Saigon Special lon 330ml',            20000, 14000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-002','Bia Tiger Crystal lon',    'Bia Tiger Crystal lon 330ml',             22000, 15000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-003','Bia Heineken lon',         'Bia Heineken lon 330ml',                  25000, 18000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-004','Bia 333 lon',              'Bia 333 lon 330ml',                       18000, 12000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-005','Bia Saigon Đỏ lon',        'Bia Saigon Đỏ lon 330ml',                 15000, 10000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-006','Két bia Saigon (24 lon)',  'Két bia Saigon Special 24 lon',          400000,295000, 'Két'),
    ('BEVERAGE','VND-001','PUB-BEV-007','Két bia Tiger (24 lon)',   'Két bia Tiger Crystal 24 lon',           440000,330000, 'Két'),
    ('BEVERAGE','VND-001','PUB-BEV-008','Rượu đế / Rượu gạo',      'Rượu gạo truyền thống chai 500ml',        50000, 30000, 'Chai'),
    ('BEVERAGE','VND-001','PUB-BEV-009','Rượu Vodka Nếp Mới',      'Vodka Nếp Mới 500ml',                    65000, 45000, 'Chai'),
    ('BEVERAGE','VND-001','PUB-BEV-010','Nước ngọt Coca-Cola',     'Coca-Cola lon 330ml',                     12000,  8000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-011','Nước ngọt Sprite',        'Sprite lon 330ml',                        12000,  8000, 'Lon'),
    ('BEVERAGE','VND-001','PUB-BEV-012','Nước suối chai',          'Nước suối chai 500ml',                     5000,  2500, 'Chai'),
    ('BEVERAGE','VND-001','PUB-BEV-013','Nước tăng lực Sting',     'Sting dâu 330ml',                         10000,  7000, 'Lon'),
    ('FOOD','VND-002','PUB-FOOD-001','Đậu phộng rang muối',        'Đậu phộng rang muối thơm đĩa nhỏ',        30000, 12000, 'Đĩa'),
    ('FOOD','VND-002','PUB-FOOD-002','Khô mực nướng',              'Khô mực nướng bơ tỏi thơm',               80000, 40000, 'Đĩa'),
    ('FOOD','VND-002','PUB-FOOD-003','Hột vịt lộn',                'Hột vịt lộn luộc kèm rau răm gừng',      15000,  8000, 'Trứng'),
    ('FOOD','VND-002','PUB-FOOD-004','Gà nướng muối ớt',           'Gà nướng muối ớt nguyên con',            180000, 90000, 'Con'),
    ('FOOD','VND-002','PUB-FOOD-005','Xúc xích nướng',             'Xúc xích nướng than hoa (5 cây)',          60000, 28000, 'Đĩa'),
    ('FOOD','VND-002','PUB-FOOD-006','Nem nướng',                  'Nem nướng cuốn rau sống (10 cái)',         70000, 35000, 'Đĩa'),
    ('FOOD','VND-002','PUB-FOOD-007','Thịt heo nướng sả',          'Thịt heo ba chỉ nướng sả ớt',             90000, 45000, 'Đĩa'),
    ('FOOD','VND-002','PUB-FOOD-008','Gỏi bắp cải',               'Gỏi bắp cải tôm thịt chua ngọt',          55000, 25000, 'Đĩa'),
    ('FOOD','VND-002','PUB-FOOD-009','Canh chua cá',               'Canh chua cá bông lau chua ngọt',         75000, 35000, 'Tô'),
    ('FOOD','VND-002','PUB-FOOD-010','Lẩu gà lá giang',            'Lẩu gà nấu lá giang chua nhẹ',           180000, 90000, 'Nồi'),
    ('FOOD','VND-002','PUB-FOOD-011','Cơm trắng',                  'Cơm trắng gạo ST25',                      10000,  3000, 'Chén'),
    ('FOOD','VND-002','PUB-FOOD-012','Bún / Bánh tráng cuốn',      'Bún + bánh tráng ăn kèm',                 15000,  5000, 'Phần')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('PUB-BEV-001', 'Bia lon & Bia chai'),
    ('PUB-BEV-002', 'Bia lon & Bia chai'),
    ('PUB-BEV-003', 'Bia lon & Bia chai'),
    ('PUB-BEV-004', 'Bia lon & Bia chai'),
    ('PUB-BEV-005', 'Bia lon & Bia chai'),
    ('PUB-BEV-006', 'Két bia'),
    ('PUB-BEV-007', 'Két bia'),
    ('PUB-BEV-008', 'Rượu mạnh'),
    ('PUB-BEV-009', 'Rượu mạnh'),
    ('PUB-BEV-010', 'Nước ngọt'),
    ('PUB-BEV-011', 'Nước ngọt'),
    ('PUB-BEV-012', 'Nước suối'),
    ('PUB-BEV-013', 'Nước ngọt'),
    ('PUB-FOOD-001', 'Mồi khô'),
    ('PUB-FOOD-002', 'Mồi khô'),
    ('PUB-FOOD-003', 'Trứng & Đặc sản'),
    ('PUB-FOOD-004', 'Mồi tươi & nướng'),
    ('PUB-FOOD-005', 'Mồi tươi & nướng'),
    ('PUB-FOOD-006', 'Mồi tươi & nướng'),
    ('PUB-FOOD-007', 'Mồi tươi & nướng'),
    ('PUB-FOOD-008', 'Đồ nhậu'),
    ('PUB-FOOD-009', 'Canh & Lẩu'),
    ('PUB-FOOD-010', 'Canh & Lẩu'),
    ('PUB-FOOD-011', 'Cơm & Bún'),
    ('PUB-FOOD-012', 'Cơm & Bún')
) AS pc(sku, cat_name)
JOIN product p ON p.sku = pc.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = pc.cat_name AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT DO NOTHING;

-- ── 6. Inventory ─────────────────────────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, status, inventory_type, last_restock_date)
SELECT
    current_setting('app.current_tenant', true),
    p.id,
    v.qty::INT, v.reorder_lvl::INT, v.reorder_qty::INT,
    v.unit_cost::NUMERIC, v.location, 'ACTIVE', 'RETAIL', NOW()
FROM (VALUES
    ('PUB-BEV-001', 240, 48, 120, 14000, 'Tủ lạnh'),
    ('PUB-BEV-002', 240, 48, 120, 15000, 'Tủ lạnh'),
    ('PUB-BEV-003', 120, 24,  72, 18000, 'Tủ lạnh'),
    ('PUB-BEV-004', 240, 48, 120, 12000, 'Tủ lạnh'),
    ('PUB-BEV-005', 240, 48, 120, 10000, 'Tủ lạnh'),
    ('PUB-BEV-006',  10,  3,   6,295000, 'Kho'),
    ('PUB-BEV-007',  10,  3,   6,330000, 'Kho'),
    ('PUB-BEV-008',  20,  5,  10, 30000, 'Kho'),
    ('PUB-BEV-009',  15,  5,  10, 45000, 'Kho'),
    ('PUB-BEV-010', 120, 24,  72,  8000, 'Tủ lạnh'),
    ('PUB-BEV-011', 120, 24,  72,  8000, 'Tủ lạnh'),
    ('PUB-BEV-012', 200, 48, 100,  2500, 'Kho'),
    ('PUB-BEV-013', 120, 24,  60,  7000, 'Tủ lạnh'),
    ('PUB-FOOD-001', 50, 10,  30, 12000, 'Bếp'),
    ('PUB-FOOD-002', 30,  8,  20, 40000, 'Kho lạnh'),
    ('PUB-FOOD-003', 60, 20,  40,  8000, 'Bếp'),
    ('PUB-FOOD-004', 10,  3,   5, 90000, 'Kho lạnh'),
    ('PUB-FOOD-005', 50, 15,  30, 28000, 'Kho lạnh'),
    ('PUB-FOOD-006', 40, 10,  25, 35000, 'Bếp'),
    ('PUB-FOOD-007', 30, 10,  20, 45000, 'Kho lạnh'),
    ('PUB-FOOD-008', 30,  8,  20, 25000, 'Bếp'),
    ('PUB-FOOD-009', 20,  5,  15, 35000, 'Bếp'),
    ('PUB-FOOD-010', 10,  3,   5, 90000, 'Kho lạnh'),
    ('PUB-FOOD-011', 200, 50, 100,  3000, 'Bếp'),
    ('PUB-FOOD-012', 100, 20,  50,  5000, 'Bếp')
) AS v(sku, qty, reorder_lvl, reorder_qty, unit_cost, location)
JOIN product p ON p.sku = v.sku AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) DO NOTHING;

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
    (current_setting('app.current_tenant', true), 'Thành viên',  0,         1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',         2000000,   1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    (current_setting('app.current_tenant', true), 'Vàng',        10000000,  1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    (current_setting('app.current_tenant', true), 'VIP',         30000000,  2.00, '#00BCD4', 'Chi tiêu từ 30 triệu VND', 4);

-- ── 9. Print templates ────────────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'POS_RECEIPT', 'Mặc định', '{
  "headerText": "",
  "footerText": "Cảm ơn quý khách!\nHẹn gặp lại!",
  "showAddress": true,
  "showTaxId": false,
  "showOrderNumber": true,
  "showDateTime": true,
  "showCustomer": false,
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

-- ── 10. Attribute groups & definitions ───────────────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'dish_info', 'Thông tin món', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'spice_level', 'Độ cay', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'prep_time_min', 'Thời gian chế biến (phút)', 'NUMBER', FALSE, FALSE, FALSE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

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

-- ── 11. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
