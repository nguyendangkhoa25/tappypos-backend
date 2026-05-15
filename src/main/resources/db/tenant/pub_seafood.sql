-- ============================================================
-- TENANT SEED — PUB_SEAFOOD / QUÁN NHẬU HẢI SẢN
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
    (current_setting('app.current_tenant', true), 'Hải sản tươi sống', NULL),
    (current_setting('app.current_tenant', true), 'Lẩu hải sản',       NULL),
    (current_setting('app.current_tenant', true), 'Đồ nhậu',           NULL),
    (current_setting('app.current_tenant', true), 'Bia & Rượu',        NULL),
    (current_setting('app.current_tenant', true), 'Nước giải khát',    NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Tôm & Cua',          'Hải sản tươi sống'),
    ('Mực & Bạch tuộc',    'Hải sản tươi sống'),
    ('Nghêu & Ốc',         'Hải sản tươi sống'),
    ('Cá tươi',            'Hải sản tươi sống'),
    ('Lẩu thập cẩm',       'Lẩu hải sản'),
    ('Lẩu đặc sản',        'Lẩu hải sản'),
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
    (current_setting('app.current_tenant', true), 'Nhà cung cấp hải sản tươi sống', 'VND-001', NULL, NULL, 'NET_3',  TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà phân phối bia & đồ uống',    'VND-002', NULL, NULL, 'NET_15', TRUE, FALSE)
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
    -- Hải sản
    ('FOOD','VND-001','SEA-001','Tôm sú nướng muối ớt',       'Tôm sú tươi nướng muối ớt thơm',                250000,150000,'Kg'),
    ('FOOD','VND-001','SEA-002','Tôm hùm hấp bia',            'Tôm hùm hấp bia mềm ngọt (tính theo kg)',       800000,550000,'Kg'),
    ('FOOD','VND-001','SEA-003','Cua rang muối',               'Cua biển rang muối ớt giòn',                    350000,220000,'Con'),
    ('FOOD','VND-001','SEA-004','Cua hấp bia',                 'Cua biển hấp bia giữ vị ngọt tự nhiên',         320000,200000,'Con'),
    ('FOOD','VND-001','SEA-005','Ghẹ hấp sả gừng',            'Ghẹ xanh hấp sả gừng thơm',                     280000,170000,'Con'),
    ('FOOD','VND-001','SEA-006','Mực chiên giòn',              'Mực ống chiên giòn bơ tỏi',                     180000,100000,'Đĩa'),
    ('FOOD','VND-001','SEA-007','Mực nướng sa tế',             'Mực nướng than sa tế cay',                      200000,115000,'Đĩa'),
    ('FOOD','VND-001','SEA-008','Bạch tuộc nướng',             'Bạch tuộc nướng than bơ tỏi',                   220000,130000,'Đĩa'),
    ('FOOD','VND-001','SEA-009','Nghêu hấp sả',                'Nghêu tươi hấp sả lá chanh',                    120000, 70000,'Kg'),
    ('FOOD','VND-001','SEA-010','Sò điệp nướng mỡ hành',       'Sò điệp nướng mỡ hành phô mai',                 200000,120000,'Đĩa'),
    ('FOOD','VND-001','SEA-011','Cá lóc nướng trui',           'Cá lóc nướng nguyên con rau sống',              200000,120000,'Con'),
    ('FOOD','VND-001','SEA-012','Cá hồi sashimi',              'Cá hồi tươi cắt sashimi ăn sống',              350000,220000,'Đĩa'),
    ('FOOD','VND-001','SEA-013','Cá mú hấp xì dầu',           'Cá mú tươi hấp xì dầu gừng hành',              400000,260000,'Con'),
    ('FOOD','VND-001','SEA-014','Lẩu hải sản thập cẩm',        'Lẩu hải sản tôm mực nghêu cua đặc biệt',        350000,200000,'Nồi'),
    ('FOOD','VND-001','SEA-015','Lẩu tôm cua',                 'Lẩu tôm cua chua cay đặc trưng',                420000,250000,'Nồi'),
    ('FOOD','VND-001','SEA-016','Lẩu mắm hải sản',             'Lẩu mắm miền Tây hải sản đủ loại',             380000,220000,'Nồi'),
    -- Đồ nhậu
    ('FOOD','VND-001','SEA-017','Đậu phộng rang muối',         'Đậu phộng rang muối thơm đĩa nhỏ',              30000, 12000,'Đĩa'),
    ('FOOD','VND-001','SEA-018','Khô mực nướng',               'Khô mực nướng bơ tỏi',                          80000, 40000,'Đĩa'),
    ('FOOD','VND-001','SEA-019','Hột vịt lộn',                 'Hột vịt lộn luộc kèm rau răm gừng',             15000,  8000,'Trứng'),
    -- Bia & Rượu
    ('BEVERAGE','VND-002','SEA-BEV-001','Bia Saigon Special lon',  'Bia Saigon Special lon 330ml',               20000, 14000,'Lon'),
    ('BEVERAGE','VND-002','SEA-BEV-002','Bia Tiger Crystal lon',   'Bia Tiger Crystal lon 330ml',                22000, 15000,'Lon'),
    ('BEVERAGE','VND-002','SEA-BEV-003','Bia Heineken lon',        'Bia Heineken lon 330ml',                     25000, 18000,'Lon'),
    ('BEVERAGE','VND-002','SEA-BEV-004','Bia 333 lon',             'Bia 333 lon 330ml',                          18000, 12000,'Lon'),
    ('BEVERAGE','VND-002','SEA-BEV-005','Két bia Saigon (24 lon)', 'Két bia Saigon Special 24 lon',            400000,295000,'Két'),
    ('BEVERAGE','VND-002','SEA-BEV-006','Rượu đế / Rượu gạo',     'Rượu gạo truyền thống chai 500ml',           50000, 30000,'Chai'),
    ('BEVERAGE','VND-002','SEA-BEV-007','Nước ngọt Coca-Cola',    'Coca-Cola lon 330ml',                         12000,  8000,'Lon'),
    ('BEVERAGE','VND-002','SEA-BEV-008','Nước suối chai',         'Nước suối chai 500ml',                          5000,  2500,'Chai')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('SEA-001', 'Tôm & Cua'),   ('SEA-002', 'Tôm & Cua'),
    ('SEA-003', 'Tôm & Cua'),   ('SEA-004', 'Tôm & Cua'),
    ('SEA-005', 'Tôm & Cua'),
    ('SEA-006', 'Mực & Bạch tuộc'),
    ('SEA-007', 'Mực & Bạch tuộc'),
    ('SEA-008', 'Mực & Bạch tuộc'),
    ('SEA-009', 'Nghêu & Ốc'),
    ('SEA-010', 'Nghêu & Ốc'),
    ('SEA-011', 'Cá tươi'),
    ('SEA-012', 'Cá tươi'),
    ('SEA-013', 'Cá tươi'),
    ('SEA-014', 'Lẩu thập cẩm'),
    ('SEA-015', 'Lẩu đặc sản'),
    ('SEA-016', 'Lẩu thập cẩm'),
    ('SEA-017', 'Mồi khô'),
    ('SEA-018', 'Mồi khô'),
    ('SEA-019', 'Mồi tươi'),
    ('SEA-BEV-001', 'Bia lon & Bia chai'),
    ('SEA-BEV-002', 'Bia lon & Bia chai'),
    ('SEA-BEV-003', 'Bia lon & Bia chai'),
    ('SEA-BEV-004', 'Bia lon & Bia chai'),
    ('SEA-BEV-005', 'Két bia'),
    ('SEA-BEV-006', 'Rượu mạnh'),
    ('SEA-BEV-007', 'Nước ngọt'),
    ('SEA-BEV-008', 'Nước suối')
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
    ('SEA-001',  10,  3,  5,150000,'Kho lạnh'),
    ('SEA-002',   5,  1,  3,550000,'Kho lạnh'),
    ('SEA-003',   8,  2,  5,220000,'Kho lạnh'),
    ('SEA-004',   8,  2,  5,200000,'Kho lạnh'),
    ('SEA-005',  10,  3,  6,170000,'Kho lạnh'),
    ('SEA-006',  20,  5, 10,100000,'Kho lạnh'),
    ('SEA-007',  20,  5, 10,115000,'Kho lạnh'),
    ('SEA-008',  15,  4,  8,130000,'Kho lạnh'),
    ('SEA-009',  15,  5,  8, 70000,'Kho lạnh'),
    ('SEA-010',  20,  5, 10,120000,'Kho lạnh'),
    ('SEA-011',  10,  3,  5,120000,'Kho lạnh'),
    ('SEA-012',   8,  2,  5,220000,'Kho lạnh'),
    ('SEA-013',   5,  2,  3,260000,'Kho lạnh'),
    ('SEA-014',  10,  3,  5,200000,'Kho lạnh'),
    ('SEA-015',  10,  3,  5,250000,'Kho lạnh'),
    ('SEA-016',  10,  3,  5,220000,'Kho lạnh'),
    ('SEA-017',  50, 10, 30, 12000,'Bếp'),
    ('SEA-018',  30,  8, 20, 40000,'Kho lạnh'),
    ('SEA-019',  60, 20, 40,  8000,'Bếp'),
    ('SEA-BEV-001', 240, 48,120, 14000,'Tủ lạnh'),
    ('SEA-BEV-002', 240, 48,120, 15000,'Tủ lạnh'),
    ('SEA-BEV-003', 120, 24, 72, 18000,'Tủ lạnh'),
    ('SEA-BEV-004', 240, 48,120, 12000,'Tủ lạnh'),
    ('SEA-BEV-005',  10,  3,  6,295000,'Kho'),
    ('SEA-BEV-006',  20,  5, 10, 30000,'Kho'),
    ('SEA-BEV-007', 120, 24, 72,  8000,'Tủ lạnh'),
    ('SEA-BEV-008', 200, 48,100,  2500,'Kho')
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
SELECT current_setting('app.current_tenant', true), id, 'seafood_info', 'Thông tin hải sản', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'seafood_type', 'Loại hải sản', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'seafood_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'cooking_method', 'Cách chế biến', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'seafood_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'price_by_weight', 'Tính giá theo kg', 'BOOLEAN', FALSE, FALSE, TRUE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'seafood_info'
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
