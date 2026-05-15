-- ============================================================
-- TENANT SEED — COFFEE SHOP / QUÁN CÀ PHÊ
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
    (current_setting('app.current_tenant', true), 'Cà phê',          NULL),
    (current_setting('app.current_tenant', true), 'Trà',             NULL),
    (current_setting('app.current_tenant', true), 'Sinh tố & Nước ép',NULL),
    (current_setting('app.current_tenant', true), 'Bánh & Snack',    NULL),
    (current_setting('app.current_tenant', true), 'Đồ uống khác',    NULL);

INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Cà phê đen',          'Cà phê'),
    ('Cà phê sữa',          'Cà phê'),
    ('Cà phê phin',         'Cà phê'),
    ('Cà phê kem & đặc biệt','Cà phê'),
    ('Trà sữa',             'Trà'),
    ('Trà trái cây',        'Trà'),
    ('Trà thảo mộc',        'Trà'),
    ('Sinh tố',             'Sinh tố & Nước ép'),
    ('Nước ép trái cây',    'Sinh tố & Nước ép'),
    ('Bánh ngọt',           'Bánh & Snack'),
    ('Bánh mì & Sandwich',  'Bánh & Snack'),
    ('Nước ngọt & Nước suối','Đồ uống khác')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp cà phê & trà',       'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp bánh & nguyên liệu', 'VND-002', NULL, NULL, 'NET_15', TRUE, FALSE)
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
    ('BEVERAGE','VND-001','CF-001','Cà phê đen đá',            'Cà phê đen pha phin, uống đá',              25000, 8000, 'Ly'),
    ('BEVERAGE','VND-001','CF-002','Cà phê đen nóng',          'Cà phê đen pha phin, uống nóng',            25000, 8000, 'Ly'),
    ('BEVERAGE','VND-001','CF-003','Cà phê sữa đá',            'Cà phê sữa đặc pha phin đá',                30000, 9000, 'Ly'),
    ('BEVERAGE','VND-001','CF-004','Cà phê sữa nóng',          'Cà phê sữa đặc pha phin nóng',             30000, 9000, 'Ly'),
    ('BEVERAGE','VND-001','CF-005','Bạc xỉu đá',               'Cà phê sữa tỉ lệ ít cà phê nhiều sữa, đá', 30000, 9000, 'Ly'),
    ('BEVERAGE','VND-001','CF-006','Bạc xỉu nóng',             'Cà phê sữa tỉ lệ ít cà phê nhiều sữa, nóng',30000,9000, 'Ly'),
    ('BEVERAGE','VND-001','CF-007','Cà phê trứng',             'Cà phê trứng kiểu Hà Nội truyền thống',     40000,12000, 'Ly'),
    ('BEVERAGE','VND-001','CF-008','Cold brew',                 'Cà phê ngâm lạnh 12 giờ nguyên chất',       45000,14000, 'Ly'),
    ('BEVERAGE','VND-001','CF-009','Cappuccino',                'Espresso + foam sữa nóng kiểu Ý',           55000,18000, 'Ly'),
    ('BEVERAGE','VND-001','CF-010','Latte',                     'Espresso + sữa tươi nóng mịn',              55000,18000, 'Ly'),
    ('BEVERAGE','VND-001','CF-011','Americano',                 'Espresso pha loãng nước nóng',              45000,14000, 'Ly'),
    ('BEVERAGE','VND-001','CF-012','Cà phê dừa',               'Cà phê đen đá kem dừa béo ngậy',            50000,16000, 'Ly'),
    ('BEVERAGE','VND-001','CF-013','Cà phê muối',              'Cà phê đen với topping kem muối',            50000,16000, 'Ly'),
    ('BEVERAGE','VND-001','TR-001','Trà sữa trân châu đen',    'Trà sữa trân châu đen pha kiểu Đài Loan',   45000,14000, 'Ly'),
    ('BEVERAGE','VND-001','TR-002','Trà sữa trân châu trắng',  'Trà sữa trân châu trắng',                   45000,14000, 'Ly'),
    ('BEVERAGE','VND-001','TR-003','Trà sữa matcha',           'Trà xanh Nhật Bản pha sữa topping trân châu',48000,15000,'Ly'),
    ('BEVERAGE','VND-001','TR-004','Trà đào cam sả đá',        'Trà đào, cam, sả, bạc hà, đá tươi mát',     45000,12000, 'Ly'),
    ('BEVERAGE','VND-001','TR-005','Trà vải',                  'Trà xanh vải thiều giải nhiệt',              40000,11000, 'Ly'),
    ('BEVERAGE','VND-001','TR-006','Trà tắc mật ong',          'Trà xanh tắc mật ong ấm',                   35000,10000, 'Ly'),
    ('BEVERAGE','VND-001','TR-007','Trà hoa cúc',              'Trà hoa cúc thanh mát dễ ngủ',              35000,10000, 'Ly'),
    ('BEVERAGE','VND-002','ST-001','Sinh tố bơ',               'Sinh tố bơ sữa đặc béo ngậy',               40000,13000, 'Ly'),
    ('BEVERAGE','VND-002','ST-002','Sinh tố dâu',              'Sinh tố dâu tây tươi',                       40000,13000, 'Ly'),
    ('BEVERAGE','VND-002','ST-003','Sinh tố xoài',             'Sinh tố xoài chín thơm',                     38000,12000, 'Ly'),
    ('BEVERAGE','VND-002','ST-004','Nước ép cam',              'Nước ép cam tươi nguyên chất',               35000,11000, 'Ly'),
    ('BEVERAGE','VND-002','ST-005','Nước ép táo',              'Nước ép táo xanh mát lạnh',                  35000,11000, 'Ly'),
    ('BEVERAGE','VND-002','NC-001','Nước suối chai',           'Nước suối chai 500ml',                        7000, 3000, 'Chai'),
    ('BEVERAGE','VND-002','NC-002','Coca-Cola lon',            'Coca-Cola lon 330ml',                         15000, 8000, 'Lon'),
    ('BEVERAGE','VND-002','NC-003','Nước tăng lực Sting',      'Nước tăng lực Sting dâu 330ml',              12000, 7000, 'Lon'),
    ('FOOD','VND-002','BK-001','Bánh tiramisu',                'Bánh tiramisu kem mascarpone cafe nguyên bản',55000,22000,'Miếng'),
    ('FOOD','VND-002','BK-002','Bánh croissant bơ',            'Bánh sừng bò bơ Pháp thơm giòn',             35000,14000,'Cái'),
    ('FOOD','VND-002','BK-003','Bánh cheesecake phô mai',      'Bánh phô mai New York mềm ngậy',            60000, 24000,'Miếng'),
    ('FOOD','VND-002','BK-004','Bánh mì sandwich trứng',       'Bánh mì sandwich trứng ốp la phô mai',      45000, 18000,'Ổ'),
    ('FOOD','VND-002','BK-005','Bánh mì bơ tỏi',              'Bánh mì nướng bơ tỏi thơm',                  25000, 10000,'Ổ'),
    ('FOOD','VND-002','BK-006','Khoai tây chiên',             'Khoai tây chiên giòn muối',                   30000, 12000,'Phần'),
    ('FOOD','VND-002','BK-007','Bánh brownie socola',          'Bánh brownie socola đen đặc ngậy',           45000, 18000,'Miếng')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('CF-001', 'Cà phê đen'),   ('CF-002', 'Cà phê đen'),
    ('CF-003', 'Cà phê sữa'),   ('CF-004', 'Cà phê sữa'),
    ('CF-005', 'Cà phê sữa'),   ('CF-006', 'Cà phê sữa'),
    ('CF-007', 'Cà phê kem & đặc biệt'),
    ('CF-008', 'Cà phê đen'),
    ('CF-009', 'Cà phê kem & đặc biệt'),
    ('CF-010', 'Cà phê kem & đặc biệt'),
    ('CF-011', 'Cà phê đen'),
    ('CF-012', 'Cà phê kem & đặc biệt'),
    ('CF-013', 'Cà phê kem & đặc biệt'),
    ('TR-001', 'Trà sữa'),  ('TR-002', 'Trà sữa'),  ('TR-003', 'Trà sữa'),
    ('TR-004', 'Trà trái cây'), ('TR-005', 'Trà trái cây'),
    ('TR-006', 'Trà thảo mộc'), ('TR-007', 'Trà thảo mộc'),
    ('ST-001', 'Sinh tố'),  ('ST-002', 'Sinh tố'),  ('ST-003', 'Sinh tố'),
    ('ST-004', 'Nước ép trái cây'), ('ST-005', 'Nước ép trái cây'),
    ('NC-001', 'Nước ngọt & Nước suối'),
    ('NC-002', 'Nước ngọt & Nước suối'),
    ('NC-003', 'Nước ngọt & Nước suối'),
    ('BK-001', 'Bánh ngọt'), ('BK-002', 'Bánh ngọt'), ('BK-003', 'Bánh ngọt'),
    ('BK-004', 'Bánh mì & Sandwich'), ('BK-005', 'Bánh mì & Sandwich'),
    ('BK-006', 'Bánh & Snack'), ('BK-007', 'Bánh ngọt')
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
    ('CF-001', 999, 0, 0,  8000, 'Quầy pha'), ('CF-002', 999, 0, 0,  8000, 'Quầy pha'),
    ('CF-003', 999, 0, 0,  9000, 'Quầy pha'), ('CF-004', 999, 0, 0,  9000, 'Quầy pha'),
    ('CF-005', 999, 0, 0,  9000, 'Quầy pha'), ('CF-006', 999, 0, 0,  9000, 'Quầy pha'),
    ('CF-007', 999, 0, 0, 12000, 'Quầy pha'), ('CF-008', 999, 0, 0, 14000, 'Quầy pha'),
    ('CF-009', 999, 0, 0, 18000, 'Quầy pha'), ('CF-010', 999, 0, 0, 18000, 'Quầy pha'),
    ('CF-011', 999, 0, 0, 14000, 'Quầy pha'), ('CF-012', 999, 0, 0, 16000, 'Quầy pha'),
    ('CF-013', 999, 0, 0, 16000, 'Quầy pha'),
    ('TR-001', 999, 0, 0, 14000, 'Quầy pha'), ('TR-002', 999, 0, 0, 14000, 'Quầy pha'),
    ('TR-003', 999, 0, 0, 15000, 'Quầy pha'), ('TR-004', 999, 0, 0, 12000, 'Quầy pha'),
    ('TR-005', 999, 0, 0, 11000, 'Quầy pha'), ('TR-006', 999, 0, 0, 10000, 'Quầy pha'),
    ('TR-007', 999, 0, 0, 10000, 'Quầy pha'),
    ('ST-001', 999, 0, 0, 13000, 'Quầy pha'), ('ST-002', 999, 0, 0, 13000, 'Quầy pha'),
    ('ST-003', 999, 0, 0, 12000, 'Quầy pha'), ('ST-004', 999, 0, 0, 11000, 'Quầy pha'),
    ('ST-005', 999, 0, 0, 11000, 'Quầy pha'),
    ('NC-001', 100, 20, 60,  3000, 'Kho'),
    ('NC-002',  60, 12, 36,  8000, 'Tủ lạnh'),
    ('NC-003',  60, 12, 36,  7000, 'Tủ lạnh'),
    ('BK-001',  20,  5, 15, 22000, 'Tủ bánh'), ('BK-002',  30,  8, 20, 14000, 'Tủ bánh'),
    ('BK-003',  20,  5, 15, 24000, 'Tủ bánh'), ('BK-004',  30,  8, 20, 18000, 'Quầy'),
    ('BK-005',  40, 10, 25, 10000, 'Quầy'),    ('BK-006',  50, 10, 30, 12000, 'Bếp'),
    ('BK-007',  20,  5, 15, 18000, 'Tủ bánh')
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
    (current_setting('app.current_tenant', true), 'Thành viên',   0,        1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',          500000,   1.25, '#9E9E9E', 'Chi tiêu từ 500K VND',     2),
    (current_setting('app.current_tenant', true), 'Vàng',         2000000,  1.50, '#FFC107', 'Chi tiêu từ 2 triệu VND',  3),
    (current_setting('app.current_tenant', true), 'VIP',          10000000, 2.00, '#00BCD4', 'Chi tiêu từ 10 triệu VND', 4);

-- ── 9. Print templates ────────────────────────────────────────
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

-- ── 10. Attribute groups & definitions (BEVERAGE) ─────────────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'bev_info', 'Thông tin đồ uống', 1
FROM product_type WHERE code = 'BEVERAGE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'bev_options', 'Tùy chọn phục vụ', 2
FROM product_type WHERE code = 'BEVERAGE' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'bev_category', 'Loại đồ uống', 'STRING', TRUE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_info'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'caffeine_content', 'Chứa caffeine', 'BOOLEAN', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_info'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'size_options', 'Kích cỡ (S/M/L)', 'STRING', FALSE, FALSE, TRUE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_options'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'sweetness_level', 'Độ ngọt', 'STRING', FALSE, FALSE, FALSE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_options'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'ice_level', 'Lượng đá', 'STRING', FALSE, FALSE, FALSE, 3
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'bev_options'
WHERE pt.code = 'BEVERAGE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- Attribute groups & definitions (FOOD)
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'food_info', 'Thông tin bánh & đồ ăn', 1
FROM product_type WHERE code = 'FOOD' AND tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_type_id, code) DO UPDATE SET display_order = EXCLUDED.display_order;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'allergens', 'Chất gây dị ứng', 'TEXT', FALSE, FALSE, FALSE, 1
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'is_vegan', 'Thuần chay', 'BOOLEAN', FALSE, FALSE, TRUE, 2
FROM product_type pt JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'food_info'
WHERE pt.code = 'FOOD' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── 11. Shop configuration ────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
