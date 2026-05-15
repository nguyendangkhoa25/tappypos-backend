-- ============================================================
-- TENANT SEED — RESTAURANT / QUÁN ĂN
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

-- ── 4. Products ───────────────────────────────────────────────
INSERT INTO product (tenant_id, product_type_id, sku, name, description, price, cost_price, unit, vendor_id, status)
SELECT
    current_setting('app.current_tenant', true),
    pt.id,
    p.sku, p.name, p.description, p.price::NUMERIC, p.cost_price::NUMERIC, p.unit,
    v.id,
    'ACTIVE'
FROM (VALUES
    ('FOOD', 'VND-001', 'RES-001', 'Phở bò tái',                'Phở bò tái chín đặc trưng',                  60000, 25000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-002', 'Phở bò viên',               'Phở bò viên thơm ngon',                      55000, 22000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-003', 'Bún bò Huế',                'Bún bò Huế cay đặc trưng miền Trung',        55000, 22000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-004', 'Hủ tiếu Nam Vang',          'Hủ tiếu heo xương hầm truyền thống',         55000, 22000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-005', 'Cơm tấm sườn bì chả',       'Cơm tấm sườn bì chả đồ chua dưa leo',       55000, 22000, 'Phần'),
    ('FOOD', 'VND-001', 'RES-006', 'Cơm gà xé phay',            'Cơm gà xé phay nước mắm chanh',              50000, 20000, 'Phần'),
    ('FOOD', 'VND-001', 'RES-007', 'Cơm chiên dương châu',       'Cơm chiên trứng thịt rau củ',                45000, 18000, 'Phần'),
    ('FOOD', 'VND-001', 'RES-008', 'Mì xào hải sản',            'Mì xào hải sản tôm mực nghêu',               65000, 28000, 'Phần'),
    ('FOOD', 'VND-001', 'RES-009', 'Gà xào sả ớt',              'Gà xào sả ớt cay thơm',                      75000, 32000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-010', 'Bò xào rau muống',          'Bò xào rau muống tỏi',                       70000, 30000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-011', 'Canh chua cá lóc',          'Canh chua cá lóc rau thơm',                  65000, 28000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-012', 'Canh khổ qua hầm',          'Canh khổ qua hầm thịt heo',                  45000, 18000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-013', 'Gỏi bắp cải tôm thịt',     'Gỏi bắp cải trộn tôm thịt rau ngò',         55000, 23000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-014', 'Nem cuốn tôm thịt',         'Nem cuốn rau thơm bánh tráng (5 cuốn)',      45000, 18000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-015', 'Chả giò thịt heo',          'Chả giò chiên giòn (5 cái)',                 40000, 16000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-016', 'Lẩu thái hải sản',          'Lẩu thái chua cay tôm mực nghêu',           220000, 95000, 'Nồi'),
    ('FOOD', 'VND-001', 'RES-017', 'Lẩu bò nhúng dấm',          'Lẩu bò nhúng dấm rau ăn kèm',              200000, 85000, 'Nồi'),
    ('FOOD', 'VND-001', 'RES-018', 'Cơm chiên trứng',           'Cơm chiên trứng hành lá đơn giản',           35000, 14000, 'Phần'),
    ('FOOD', 'VND-001', 'RES-019', 'Rau muống xào tỏi',         'Rau muống xào tỏi phi thơm',                 35000, 12000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-020', 'Trứng chiên hành',          'Trứng chiên hành 2 trứng',                   25000, 10000, 'Đĩa'),
    ('FOOD', 'VND-001', 'RES-021', 'Cơm trắng',                 'Cơm trắng nấu từ gạo ST25',                  10000,  3000, 'Chén'),
    ('FOOD', 'VND-001', 'RES-022', 'Bánh mì thịt',              'Bánh mì thịt nguội pa tê chả lụa',           30000, 12000, 'Ổ'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-001', 'Bia Tiger lon',     'Bia Tiger 330ml lon',                         22000, 16000, 'Lon'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-002', 'Bia Saigon Special','Bia Saigon Special 330ml lon',                20000, 14000, 'Lon'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-003', 'Bia Heineken lon',  'Bia Heineken 330ml lon',                      25000, 18000, 'Lon'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-004', 'Nước suối chai',    'Nước suối chai 500ml',                         5000,  2500, 'Chai'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-005', 'Nước ngọt Coca-Cola','Coca-Cola lon 330ml',                        12000,  8000, 'Lon'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-006', 'Nước ngọt 7-Up',   '7-Up lon 330ml',                              11000,  7500, 'Lon'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-007', 'Trà đá',            'Trà đá giải khát miễn phí / có phí',          5000,  1000, 'Ly'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-008', 'Nước cam tươi',     'Nước cam vắt tươi nguyên chất',              25000, 10000, 'Ly'),
    ('BEVERAGE', 'VND-002', 'RES-BEV-009', 'Sinh tố bơ',        'Sinh tố bơ sữa đặc béo ngậy',               35000, 15000, 'Ly'),
    ('FOOD', 'VND-001', 'RES-023', 'Cơm tấm đặc biệt',          'Cơm tấm sườn bì chả ốp la',                  65000, 28000, 'Phần'),
    ('FOOD', 'VND-001', 'RES-024', 'Bún thịt nướng',            'Bún thịt nướng chả giò đồ chua',             60000, 25000, 'Tô'),
    ('FOOD', 'VND-001', 'RES-025', 'Cháo gà',                   'Cháo gà xé hành ngò rí',                     45000, 18000, 'Tô')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 5. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('RES-001', 'Phở & Bún bò'),
    ('RES-002', 'Phở & Bún bò'),
    ('RES-003', 'Phở & Bún bò'),
    ('RES-004', 'Mì & Hủ tiếu'),
    ('RES-005', 'Cơm phần'),
    ('RES-006', 'Cơm phần'),
    ('RES-007', 'Cơm chiên'),
    ('RES-008', 'Mì & Hủ tiếu'),
    ('RES-009', 'Món xào'),
    ('RES-010', 'Món xào'),
    ('RES-011', 'Canh & Súp'),
    ('RES-012', 'Canh & Súp'),
    ('RES-013', 'Gỏi & Salad'),
    ('RES-014', 'Khai vị'),
    ('RES-015', 'Khai vị'),
    ('RES-016', 'Lẩu'),
    ('RES-017', 'Lẩu'),
    ('RES-018', 'Cơm chiên'),
    ('RES-019', 'Món xào'),
    ('RES-020', 'Món chính'),
    ('RES-021', 'Cơm phần'),
    ('RES-022', 'Khai vị'),
    ('RES-BEV-001', 'Bia & Rượu'),
    ('RES-BEV-002', 'Bia & Rượu'),
    ('RES-BEV-003', 'Bia & Rượu'),
    ('RES-BEV-004', 'Nước suối'),
    ('RES-BEV-005', 'Nước ngọt'),
    ('RES-BEV-006', 'Nước ngọt'),
    ('RES-BEV-007', 'Trà & Cà phê'),
    ('RES-BEV-008', 'Đồ uống'),
    ('RES-BEV-009', 'Đồ uống'),
    ('RES-023', 'Món đặc biệt'),
    ('RES-024', 'Phở & Bún bò'),
    ('RES-025', 'Cơm & Bún & Phở')
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
    ('RES-001', 50, 10, 30, 25000, 'Bếp'),
    ('RES-002', 50, 10, 30, 22000, 'Bếp'),
    ('RES-003', 50, 10, 30, 22000, 'Bếp'),
    ('RES-004', 50, 10, 30, 22000, 'Bếp'),
    ('RES-005', 80, 15, 40, 22000, 'Bếp'),
    ('RES-006', 80, 15, 40, 20000, 'Bếp'),
    ('RES-007', 80, 15, 40, 18000, 'Bếp'),
    ('RES-008', 50, 10, 30, 28000, 'Bếp'),
    ('RES-009', 40, 10, 20, 32000, 'Bếp'),
    ('RES-010', 40, 10, 20, 30000, 'Bếp'),
    ('RES-011', 30, 10, 20, 28000, 'Bếp'),
    ('RES-012', 30, 10, 20, 18000, 'Bếp'),
    ('RES-013', 30,  8, 20, 23000, 'Bếp'),
    ('RES-014', 40, 10, 25, 18000, 'Bếp'),
    ('RES-015', 50, 10, 30, 16000, 'Bếp'),
    ('RES-016', 20,  5, 10, 95000, 'Kho lạnh'),
    ('RES-017', 20,  5, 10, 85000, 'Kho lạnh'),
    ('RES-018', 60, 15, 40, 14000, 'Bếp'),
    ('RES-019', 60, 15, 40, 12000, 'Bếp'),
    ('RES-020', 80, 20, 50, 10000, 'Bếp'),
    ('RES-021', 200, 50, 100, 3000, 'Bếp'),
    ('RES-022', 50, 10, 30, 12000, 'Quầy'),
    ('RES-BEV-001', 120, 24, 72, 16000, 'Tủ lạnh'),
    ('RES-BEV-002', 120, 24, 72, 14000, 'Tủ lạnh'),
    ('RES-BEV-003',  60, 12, 36, 18000, 'Tủ lạnh'),
    ('RES-BEV-004', 200, 48, 96,  2500, 'Kho'),
    ('RES-BEV-005',  80, 24, 48,  8000, 'Tủ lạnh'),
    ('RES-BEV-006',  80, 24, 48,  7500, 'Tủ lạnh'),
    ('RES-BEV-007', 999,  0,  0,  1000, 'Bàn pha'),
    ('RES-BEV-008',  50, 10, 30, 10000, 'Bàn pha'),
    ('RES-BEV-009',  40, 10, 25, 15000, 'Bàn pha'),
    ('RES-023', 60, 15, 40, 28000, 'Bếp'),
    ('RES-024', 50, 10, 30, 25000, 'Bếp'),
    ('RES-025', 40, 10, 25, 18000, 'Bếp')
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
