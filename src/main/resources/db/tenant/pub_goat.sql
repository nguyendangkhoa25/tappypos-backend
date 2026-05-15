-- ============================================================
-- TENANT SEED — PUB_GOAT / QUÁN NHẬU CHUYÊN DÊ
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
    (current_setting('app.current_tenant', true), 'Thịt dê',       NULL),
    (current_setting('app.current_tenant', true), 'Lẩu dê',        NULL),
    (current_setting('app.current_tenant', true), 'Đồ nhậu',       NULL),
    (current_setting('app.current_tenant', true), 'Bia & Rượu',    NULL),
    (current_setting('app.current_tenant', true), 'Nước giải khát',NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Dê nướng',           'Thịt dê'),
    ('Dê xào & hấp',       'Thịt dê'),
    ('Dê đặc sản',         'Thịt dê'),
    ('Lẩu dê thường',      'Lẩu dê'),
    ('Lẩu dê đặc biệt',   'Lẩu dê'),
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
    (current_setting('app.current_tenant', true), 'Trang trại dê & thịt sạch',   'VND-001', NULL, NULL, 'NET_7',  TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà phân phối bia & đồ uống', 'VND-002', NULL, NULL, 'NET_15', TRUE, FALSE)
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
    -- Thịt dê
    ('FOOD','VND-001','GOAT-001','Thịt dê xào lăn',            'Thịt dê xào lăn sả ớt gừng thơm',            200000,120000,'Đĩa'),
    ('FOOD','VND-001','GOAT-002','Dê nướng nguyên con',         'Dê nướng nguyên con than hoa đặt trước',     1500000,900000,'Con'),
    ('FOOD','VND-001','GOAT-003','Dê nướng bếp than (phần)',    'Dê nướng bếp than hoa đĩa 500g',             250000,150000,'Đĩa'),
    ('FOOD','VND-001','GOAT-004','Dê hấp gừng',                 'Thịt dê hấp gừng sả thơm nức',               220000,130000,'Đĩa'),
    ('FOOD','VND-001','GOAT-005','Dê sốt vang',                 'Thịt dê hầm rượu vang đỏ kiểu Pháp',         200000,120000,'Đĩa'),
    ('FOOD','VND-001','GOAT-006','Dồi dê nướng',                'Dồi dê nhồi huyết nướng than thơm',          150000, 90000,'Đĩa'),
    ('FOOD','VND-001','GOAT-007','Tiết canh dê',                'Tiết canh dê tươi ăn kèm rau húng',           80000, 50000,'Bát'),
    ('FOOD','VND-001','GOAT-008','Cháo dê hầm',                 'Cháo dê hầm gừng nghệ bổ dưỡng',            100000, 60000,'Tô'),
    ('FOOD','VND-001','GOAT-009','Dê tái chanh',                'Thịt dê tái chanh gừng rau thơm',            180000,110000,'Đĩa'),
    ('FOOD','VND-001','GOAT-010','Lẩu dê',                      'Lẩu dê sả ớt bắp cải rau thơm',             350000,200000,'Nồi'),
    ('FOOD','VND-001','GOAT-011','Lẩu dê sốt vang',             'Lẩu dê hầm rượu vang đỏ đặc biệt',          450000,270000,'Nồi'),
    -- Đồ nhậu
    ('FOOD','VND-001','GOAT-012','Đậu phộng rang muối',         'Đậu phộng rang muối thơm đĩa nhỏ',           30000, 12000,'Đĩa'),
    ('FOOD','VND-001','GOAT-013','Khô mực nướng',               'Khô mực nướng bơ tỏi',                        80000, 40000,'Đĩa'),
    ('FOOD','VND-001','GOAT-014','Hột vịt lộn',                 'Hột vịt lộn luộc kèm rau răm gừng',          15000,  8000,'Trứng'),
    -- Bia & Rượu
    ('BEVERAGE','VND-002','GOAT-BEV-001','Bia Saigon Special lon', 'Bia Saigon Special lon 330ml',             20000, 14000,'Lon'),
    ('BEVERAGE','VND-002','GOAT-BEV-002','Bia Tiger Crystal lon',  'Bia Tiger Crystal lon 330ml',              22000, 15000,'Lon'),
    ('BEVERAGE','VND-002','GOAT-BEV-003','Bia Heineken lon',       'Bia Heineken lon 330ml',                   25000, 18000,'Lon'),
    ('BEVERAGE','VND-002','GOAT-BEV-004','Bia 333 lon',            'Bia 333 lon 330ml',                        18000, 12000,'Lon'),
    ('BEVERAGE','VND-002','GOAT-BEV-005','Két bia Saigon (24 lon)','Két bia Saigon Special 24 lon',          400000,295000,'Két'),
    ('BEVERAGE','VND-002','GOAT-BEV-006','Rượu đế / Rượu gạo',    'Rượu gạo truyền thống chai 500ml',         50000, 30000,'Chai'),
    ('BEVERAGE','VND-002','GOAT-BEV-007','Rượu Vodka',             'Vodka Hà Nội 500ml',                       60000, 40000,'Chai'),
    ('BEVERAGE','VND-002','GOAT-BEV-008','Nước ngọt Coca-Cola',   'Coca-Cola lon 330ml',                       12000,  8000,'Lon'),
    ('BEVERAGE','VND-002','GOAT-BEV-009','Nước suối chai',        'Nước suối chai 500ml',                       5000,  2500,'Chai')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('GOAT-001', 'Dê xào & hấp'),
    ('GOAT-002', 'Dê nướng'),
    ('GOAT-003', 'Dê nướng'),
    ('GOAT-004', 'Dê xào & hấp'),
    ('GOAT-005', 'Dê đặc sản'),
    ('GOAT-006', 'Dê đặc sản'),
    ('GOAT-007', 'Dê đặc sản'),
    ('GOAT-008', 'Dê đặc sản'),
    ('GOAT-009', 'Dê xào & hấp'),
    ('GOAT-010', 'Lẩu dê thường'),
    ('GOAT-011', 'Lẩu dê đặc biệt'),
    ('GOAT-012', 'Mồi khô'),
    ('GOAT-013', 'Mồi khô'),
    ('GOAT-014', 'Mồi tươi'),
    ('GOAT-BEV-001', 'Bia lon & Bia chai'),
    ('GOAT-BEV-002', 'Bia lon & Bia chai'),
    ('GOAT-BEV-003', 'Bia lon & Bia chai'),
    ('GOAT-BEV-004', 'Bia lon & Bia chai'),
    ('GOAT-BEV-005', 'Két bia'),
    ('GOAT-BEV-006', 'Rượu mạnh'),
    ('GOAT-BEV-007', 'Rượu mạnh'),
    ('GOAT-BEV-008', 'Nước ngọt'),
    ('GOAT-BEV-009', 'Nước suối')
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
    ('GOAT-001',  20,  5, 10,120000,'Kho lạnh'),
    ('GOAT-002',   2,  1,  1,900000,'Kho lạnh'),
    ('GOAT-003',  15,  4,  8,150000,'Kho lạnh'),
    ('GOAT-004',  15,  4,  8,130000,'Kho lạnh'),
    ('GOAT-005',  15,  4,  8,120000,'Kho lạnh'),
    ('GOAT-006',  20,  5, 10, 90000,'Kho lạnh'),
    ('GOAT-007',  20,  8, 15, 50000,'Kho lạnh'),
    ('GOAT-008',  20,  5, 10, 60000,'Bếp'),
    ('GOAT-009',  15,  4,  8,110000,'Kho lạnh'),
    ('GOAT-010',  10,  3,  5,200000,'Kho lạnh'),
    ('GOAT-011',  10,  3,  5,270000,'Kho lạnh'),
    ('GOAT-012',  50, 10, 30, 12000,'Bếp'),
    ('GOAT-013',  30,  8, 20, 40000,'Kho lạnh'),
    ('GOAT-014',  60, 20, 40,  8000,'Bếp'),
    ('GOAT-BEV-001', 240, 48,120, 14000,'Tủ lạnh'),
    ('GOAT-BEV-002', 240, 48,120, 15000,'Tủ lạnh'),
    ('GOAT-BEV-003', 120, 24, 72, 18000,'Tủ lạnh'),
    ('GOAT-BEV-004', 240, 48,120, 12000,'Tủ lạnh'),
    ('GOAT-BEV-005',  10,  3,  6,295000,'Kho'),
    ('GOAT-BEV-006',  20,  5, 10, 30000,'Kho'),
    ('GOAT-BEV-007',  15,  5, 10, 40000,'Kho'),
    ('GOAT-BEV-008', 120, 24, 72,  8000,'Tủ lạnh'),
    ('GOAT-BEV-009', 200, 48,100,  2500,'Kho')
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
SELECT current_setting('app.current_tenant', true), id, 'dish_info', 'Thông tin món dê', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'goat_part', 'Phần thịt dê', 'STRING', FALSE, FALSE, TRUE, 1
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
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'spice_level', 'Độ cay', 'STRING', FALSE, FALSE, TRUE, 3
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
