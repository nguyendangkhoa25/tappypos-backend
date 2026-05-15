-- ============================================================
-- TENANT SEED — PUB_BEEF / QUÁN NHẬU CHUYÊN BÒ
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
    (current_setting('app.current_tenant', true), 'Thịt bò',       NULL),
    (current_setting('app.current_tenant', true), 'Lẩu bò',        NULL),
    (current_setting('app.current_tenant', true), 'Đồ nhậu',       NULL),
    (current_setting('app.current_tenant', true), 'Bia & Rượu',    NULL),
    (current_setting('app.current_tenant', true), 'Nước giải khát',NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Bò nướng',           'Thịt bò'),
    ('Bò xào & nhúng',     'Thịt bò'),
    ('Bò hầm & kho',       'Thịt bò'),
    ('Bò đặc sản',         'Thịt bò'),
    ('Lẩu bò thường',      'Lẩu bò'),
    ('Lẩu bò đặc biệt',   'Lẩu bò'),
    ('Mồi khô',            'Đồ nhậu'),
    ('Mồi tươi',           'Đồ nhậu'),
    ('Bia lon & Bia chai',  'Bia & Rượu'),
    ('Két bia',             'Bia & Rượu'),
    ('Rượu mạnh',           'Bia & Rượu'),
    ('Nước ngọt',          'Nước giải khát'),
    ('Nước suối',          'Nước giải khát')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp thịt bò sạch',    'VND-001', NULL, NULL, 'NET_7',  TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà phân phối bia & đồ uống',  'VND-002', NULL, NULL, 'NET_15', TRUE, FALSE)
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
    -- Thịt bò
    ('FOOD','VND-001','BEEF-001','Bò nhúng dấm',               'Bò tái nhúng dấm cuốn rau sống bánh tráng',  280000,170000,'Phần'),
    ('FOOD','VND-001','BEEF-002','Bò nướng ngũ vị',            'Bò nướng ngũ vị hương than hoa thơm',        250000,150000,'Đĩa'),
    ('FOOD','VND-001','BEEF-003','Bò nướng lá lốt',            'Chả bò cuốn lá lốt nướng than (10 cuốn)',    180000,110000,'Đĩa'),
    ('FOOD','VND-001','BEEF-004','Bò tái chanh',               'Bò tái chanh sả ớt gừng rau thơm',           170000,100000,'Đĩa'),
    ('FOOD','VND-001','BEEF-005','Bò xào rau cải',             'Bò xào rau cải tỏi xanh mềm ngon',           160000, 95000,'Đĩa'),
    ('FOOD','VND-001','BEEF-006','Bò xào sả ớt',               'Bò xào sả ớt cay thơm',                      170000,100000,'Đĩa'),
    ('FOOD','VND-001','BEEF-007','Bắp bò kho gừng',            'Bắp bò kho gừng mềm thấm vị',               180000,110000,'Đĩa'),
    ('FOOD','VND-001','BEEF-008','Gân bò hầm',                  'Gân bò hầm sa tế mềm béo',                   150000, 90000,'Đĩa'),
    ('FOOD','VND-001','BEEF-009','Bò kho bánh mì',             'Bò kho hầm mềm ăn kèm bánh mì',             120000, 70000,'Tô'),
    ('FOOD','VND-001','BEEF-010','Lòng bò xào sả ớt',         'Lòng bò xào sả ớt giòn thơm',                130000, 75000,'Đĩa'),
    ('FOOD','VND-001','BEEF-011','Lẩu bò',                     'Lẩu bò nhúng rau cải nấm đặc trưng',         320000,190000,'Nồi'),
    ('FOOD','VND-001','BEEF-012','Lẩu bò nhúng dấm',           'Lẩu bò nhúng dấm cuốn rau sống',            380000,230000,'Nồi'),
    ('FOOD','VND-001','BEEF-013','Lẩu bò thái đặc biệt',       'Lẩu bò thái chua cay hải sản kết hợp',      420000,260000,'Nồi'),
    -- Đồ nhậu
    ('FOOD','VND-001','BEEF-014','Đậu phộng rang muối',        'Đậu phộng rang muối thơm đĩa nhỏ',           30000, 12000,'Đĩa'),
    ('FOOD','VND-001','BEEF-015','Khô bò gác bếp',             'Khô bò gác bếp Tây Bắc dẻo thơm',           120000, 75000,'Đĩa'),
    ('FOOD','VND-001','BEEF-016','Hột vịt lộn',                'Hột vịt lộn luộc kèm rau răm gừng',          15000,  8000,'Trứng'),
    -- Bia & Rượu
    ('BEVERAGE','VND-002','BEEF-BEV-001','Bia Saigon Special lon', 'Bia Saigon Special lon 330ml',            20000, 14000,'Lon'),
    ('BEVERAGE','VND-002','BEEF-BEV-002','Bia Tiger Crystal lon',  'Bia Tiger Crystal lon 330ml',             22000, 15000,'Lon'),
    ('BEVERAGE','VND-002','BEEF-BEV-003','Bia Heineken lon',       'Bia Heineken lon 330ml',                  25000, 18000,'Lon'),
    ('BEVERAGE','VND-002','BEEF-BEV-004','Bia 333 lon',            'Bia 333 lon 330ml',                       18000, 12000,'Lon'),
    ('BEVERAGE','VND-002','BEEF-BEV-005','Két bia Saigon (24 lon)','Két bia Saigon Special 24 lon',         400000,295000,'Két'),
    ('BEVERAGE','VND-002','BEEF-BEV-006','Rượu đế / Rượu gạo',    'Rượu gạo truyền thống chai 500ml',        50000, 30000,'Chai'),
    ('BEVERAGE','VND-002','BEEF-BEV-007','Rượu Whisky đá',         'Whisky Johnnie Walker Red uống đá',       80000, 55000,'Ly'),
    ('BEVERAGE','VND-002','BEEF-BEV-008','Nước ngọt Coca-Cola',   'Coca-Cola lon 330ml',                      12000,  8000,'Lon'),
    ('BEVERAGE','VND-002','BEEF-BEV-009','Nước suối chai',        'Nước suối chai 500ml',                       5000,  2500,'Chai')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('BEEF-001', 'Bò xào & nhúng'),
    ('BEEF-002', 'Bò nướng'),
    ('BEEF-003', 'Bò nướng'),
    ('BEEF-004', 'Bò đặc sản'),
    ('BEEF-005', 'Bò xào & nhúng'),
    ('BEEF-006', 'Bò xào & nhúng'),
    ('BEEF-007', 'Bò hầm & kho'),
    ('BEEF-008', 'Bò hầm & kho'),
    ('BEEF-009', 'Bò hầm & kho'),
    ('BEEF-010', 'Bò đặc sản'),
    ('BEEF-011', 'Lẩu bò thường'),
    ('BEEF-012', 'Lẩu bò đặc biệt'),
    ('BEEF-013', 'Lẩu bò đặc biệt'),
    ('BEEF-014', 'Mồi khô'),
    ('BEEF-015', 'Mồi khô'),
    ('BEEF-016', 'Mồi tươi'),
    ('BEEF-BEV-001', 'Bia lon & Bia chai'),
    ('BEEF-BEV-002', 'Bia lon & Bia chai'),
    ('BEEF-BEV-003', 'Bia lon & Bia chai'),
    ('BEEF-BEV-004', 'Bia lon & Bia chai'),
    ('BEEF-BEV-005', 'Két bia'),
    ('BEEF-BEV-006', 'Rượu mạnh'),
    ('BEEF-BEV-007', 'Rượu mạnh'),
    ('BEEF-BEV-008', 'Nước ngọt'),
    ('BEEF-BEV-009', 'Nước suối')
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
    ('BEEF-001',  20,  5, 10,170000,'Kho lạnh'),
    ('BEEF-002',  20,  5, 10,150000,'Kho lạnh'),
    ('BEEF-003',  25,  6, 12,110000,'Kho lạnh'),
    ('BEEF-004',  20,  5, 10,100000,'Kho lạnh'),
    ('BEEF-005',  20,  5, 10, 95000,'Kho lạnh'),
    ('BEEF-006',  20,  5, 10,100000,'Kho lạnh'),
    ('BEEF-007',  15,  4,  8,110000,'Kho lạnh'),
    ('BEEF-008',  15,  4,  8, 90000,'Kho lạnh'),
    ('BEEF-009',  25,  6, 12, 70000,'Bếp'),
    ('BEEF-010',  20,  5, 10, 75000,'Kho lạnh'),
    ('BEEF-011',  10,  3,  5,190000,'Kho lạnh'),
    ('BEEF-012',  10,  3,  5,230000,'Kho lạnh'),
    ('BEEF-013',  10,  3,  5,260000,'Kho lạnh'),
    ('BEEF-014',  50, 10, 30, 12000,'Bếp'),
    ('BEEF-015',  25,  6, 15, 75000,'Kho lạnh'),
    ('BEEF-016',  60, 20, 40,  8000,'Bếp'),
    ('BEEF-BEV-001', 240, 48,120, 14000,'Tủ lạnh'),
    ('BEEF-BEV-002', 240, 48,120, 15000,'Tủ lạnh'),
    ('BEEF-BEV-003', 120, 24, 72, 18000,'Tủ lạnh'),
    ('BEEF-BEV-004', 240, 48,120, 12000,'Tủ lạnh'),
    ('BEEF-BEV-005',  10,  3,  6,295000,'Kho'),
    ('BEEF-BEV-006',  20,  5, 10, 30000,'Kho'),
    ('BEEF-BEV-007',  12,  3,  6, 55000,'Quầy'),
    ('BEEF-BEV-008', 120, 24, 72,  8000,'Tủ lạnh'),
    ('BEEF-BEV-009', 200, 48,100,  2500,'Kho')
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
SELECT current_setting('app.current_tenant', true), id, 'dish_info', 'Thông tin món bò', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'beef_part', 'Phần thịt bò', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'cooking_method', 'Cách chế biến', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'dish_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'doneness', 'Độ chín (tái/chín)', 'STRING', FALSE, FALSE, TRUE, 3
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
