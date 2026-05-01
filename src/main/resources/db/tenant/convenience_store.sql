-- ============================================================
-- TENANT SEED — DEFAULT DATA: CONVENIENCE STORE (TẠP HÓA)
-- PostgreSQL / shared-DB version.
-- All INSERTs are idempotent (ON CONFLICT DO NOTHING or DO UPDATE).
-- tenant_id sourced from app.current_tenant session variable.
-- ============================================================

-- ── 1. Product types (18 standard types) ─────────────────────
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
-- Parent categories
INSERT INTO category (tenant_id, name, parent_id) VALUES
    (current_setting('app.current_tenant', true), 'Đồ uống',                     NULL),
    (current_setting('app.current_tenant', true), 'Thực phẩm',                   NULL),
    (current_setting('app.current_tenant', true), 'Bánh kẹo & Snacks',           NULL),
    (current_setting('app.current_tenant', true), 'Vệ sinh cá nhân',             NULL),
    (current_setting('app.current_tenant', true), 'Đồ gia dụng',                 NULL),
    (current_setting('app.current_tenant', true), 'Thuốc lá',                    NULL),
    (current_setting('app.current_tenant', true), 'Sữa & Sản phẩm sữa',         NULL),
    (current_setting('app.current_tenant', true), 'Gia vị & Nước chấm',          NULL);

-- Child categories
INSERT INTO category (tenant_id, name, parent_id)
SELECT current_setting('app.current_tenant', true), c.name, p.id
FROM (VALUES
    ('Nước giải khát',              'Đồ uống'),
    ('Nước suối / Nước tinh khiết', 'Đồ uống'),
    ('Bia & Nước có cồn',           'Đồ uống'),
    ('Trà & Cà phê đóng gói',       'Đồ uống'),
    ('Nước tăng lực',               'Đồ uống'),
    ('Mì gói & Cháo gói',           'Thực phẩm'),
    ('Thực phẩm khô',               'Thực phẩm'),
    ('Bánh quy & Bánh ngọt',        'Bánh kẹo & Snacks'),
    ('Kẹo & Socola',                'Bánh kẹo & Snacks'),
    ('Snack khoai tây',             'Bánh kẹo & Snacks')
) AS c(name, parent_name)
JOIN category p ON p.name = c.parent_name
    AND p.tenant_id = current_setting('app.current_tenant', true)
    AND p.parent_id IS NULL;

-- ── 3. Walk-in customer ───────────────────────────────────────
INSERT INTO customers (tenant_id, name, phone, email, notes, deleted)
VALUES (current_setting('app.current_tenant', true), 'Khách lẻ', '0000000000', NULL,
        'Khách hàng lẻ - không có thông tin liên hệ', FALSE)
ON CONFLICT (phone, tenant_id) DO NOTHING;

-- ── 4. Vendors ────────────────────────────────────────────────
INSERT INTO vendors (tenant_id, name, code, contact_name, phone, payment_terms, is_active, deleted)
VALUES
    (current_setting('app.current_tenant', true), 'Nhà cung cấp đồ uống & FMCG',         'VND-001', NULL, NULL, 'NET_30', TRUE, FALSE),
    (current_setting('app.current_tenant', true), 'Nhà cung cấp thực phẩm & tiêu dùng',  'VND-002', NULL, NULL, 'NET_30', TRUE, FALSE)
ON CONFLICT (code, tenant_id) DO NOTHING;

-- ── 5. Products (35 common convenience store items) ──────────
INSERT INTO product (tenant_id, product_type_id, sku, name, description, price, cost_price, unit, vendor_id, status)
SELECT
    current_setting('app.current_tenant', true),
    pt.id,
    p.sku, p.name, p.description, p.price::NUMERIC, p.cost_price::NUMERIC, p.unit,
    v.id,
    'ACTIVE'
FROM (VALUES
    ('BEVERAGE', 'VND-001', 'BEV-001', 'Coca-Cola 330ml',              'Nước ngọt có ga lon 330ml',               12000,  8500, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-002', 'Pepsi 330ml',                  'Nước ngọt có ga lon 330ml',               12000,  8500, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-003', '7-Up 330ml',                   'Nước ngọt có ga lon 330ml',               11000,  7500, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-004', 'Red Bull 250ml',               'Nước tăng lực lon 250ml',                 13000,  9500, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-005', 'Number One 330ml',             'Nước tăng lực lon 330ml',                 10000,  7000, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-006', 'Nước suối Aqua 500ml',         'Nước tinh khiết chai 500ml',               6000,  3500, 'chai'),
    ('BEVERAGE', 'VND-001', 'BEV-007', 'Nước suối La Vie 500ml',       'Nước khoáng thiên nhiên chai 500ml',       7000,  4500, 'chai'),
    ('BEVERAGE', 'VND-001', 'BEV-008', 'Bia Tiger 330ml',              'Bia lon 330ml',                           18000, 13000, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-009', 'Bia Heineken 330ml',           'Bia lon 330ml',                           25000, 18000, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-010', 'Bia Saigon Đỏ 330ml',         'Bia lon 330ml',                           15000, 11000, 'lon'),
    ('BEVERAGE', 'VND-001', 'BEV-011', 'Trà Olong Tea Plus 455ml',     'Trà Olong chai PET 455ml',                12000,  8000, 'chai'),
    ('BEVERAGE', 'VND-001', 'BEV-012', 'Trà xanh 0 Độ 455ml',         'Trà xanh không đường chai 455ml',         12000,  8000, 'chai'),
    ('BEVERAGE', 'VND-001', 'BEV-013', 'Sting Dâu 330ml',             'Nước tăng lực hương dâu lon 330ml',       10000,  7000, 'lon'),
    ('FOOD',     'VND-001', 'FOOD-001', 'Mì Hảo Hảo Tôm Chua Cay 75g','Mì ăn liền gói 75g',                      7000,  4800, 'gói'),
    ('FOOD',     'VND-001', 'FOOD-002', 'Mì 3 Miền Bò Hầm Rau 65g',   'Mì ăn liền gói 65g',                      6000,  4000, 'gói'),
    ('FOOD',     'VND-001', 'FOOD-003', 'Phở Bò Ăn Liền Vifon 65g',   'Phở ăn liền gói 65g',                     8000,  5500, 'gói'),
    ('FOOD',     'VND-001', 'FOOD-004', 'Cháo Thịt Bằm Vifon 50g',    'Cháo ăn liền gói 50g',                   12000,  8500, 'gói'),
    ('FOOD',     'VND-002', 'FOOD-005', 'Sữa TH True Milk 180ml',      'Sữa tươi tiệt trùng hộp',                 8000,  5800, 'hộp'),
    ('FOOD',     'VND-002', 'FOOD-006', 'Sữa Vinamilk UHT 180ml',      'Sữa tươi UHT hộp 180ml',                  7500,  5200, 'hộp'),
    ('FOOD',     'VND-002', 'FOOD-007', 'Nước mắm Nam Ngư 500ml',      'Nước mắm chai thuỷ tinh 500ml',           18000, 12500, 'chai'),
    ('FOOD',     'VND-002', 'FOOD-008', 'Dầu ăn Neptune 500ml',        'Dầu thực vật chai 500ml',                 42000, 31000, 'chai'),
    ('FOOD',     'VND-002', 'FOOD-009', 'Mì chính Ajinomoto 100g',     'Bột ngọt gói 100g',                       12000,  8500, 'gói'),
    ('CONVENIENCE','VND-002','CONV-001','Bánh Oreo 97g',               'Bánh quy kem 97g',                        18000, 13000, 'gói'),
    ('CONVENIENCE','VND-002','CONV-002','Snack Poca Khoai Tây BBQ 68g','Snack khoai tây vị BBQ gói 68g',          12000,  8500, 'gói'),
    ('CONVENIENCE','VND-002','CONV-003','Kẹo Dừa Bến Tre 200g',        'Kẹo dừa truyền thống gói 200g',           25000, 18000, 'gói'),
    ('CONVENIENCE','VND-002','CONV-004','Bánh Kinh Đô Hương Vani',     'Bánh quy bơ hộp 150g',                    22000, 15000, 'hộp'),
    ('BEAUTY',   'VND-001', 'BEAU-001', 'Kem đánh răng Colgate 150g',  'Kem đánh răng bảo vệ toàn diện',          35000, 25000, 'tuýp'),
    ('BEAUTY',   'VND-001', 'BEAU-002', 'Dầu gội Clear Mát Lạnh 170ml','Dầu gội sạch gàu chai 170ml',             45000, 33000, 'chai'),
    ('BEAUTY',   'VND-001', 'BEAU-003', 'Xà phòng Lifebuoy 90g',       'Xà phòng kháng khuẩn 90g',                18000, 13000, 'bánh'),
    ('BEAUTY',   'VND-001', 'BEAU-004', 'Bàn chải Oral-B Classic',     'Bàn chải đánh răng lông mềm',             25000, 18000, 'cái'),
    ('CONVENIENCE','VND-002','CONV-005','Nước rửa chén Sunlight 500ml','Nước rửa chén chai 500ml',                22000, 16000, 'chai'),
    ('CONVENIENCE','VND-002','CONV-006','Bột giặt Omo Comfort 400g',   'Bột giặt thơm gói 400g',                  32000, 24000, 'gói'),
    ('CONVENIENCE','VND-002','CONV-007','Túi nilon HDPE 30 cái',       'Túi đựng hàng 30x40cm, 30 cái/cuộn',      5000,  3000, 'cuộn'),
    ('CONVENIENCE','VND-002','CONV-008','Thuốc lá Esse Menthol',       'Thuốc lá Esse bạc hà 1 bao',              30000, 25000, 'bao'),
    ('CONVENIENCE','VND-002','CONV-009','Thuốc lá 555 State Express',  'Thuốc lá 555 1 bao',                      35000, 30000, 'bao')
) AS p(type_code, vend_code, sku, name, description, price, cost_price, unit)
JOIN product_type pt ON pt.code = p.type_code AND pt.tenant_id = current_setting('app.current_tenant', true)
JOIN vendors v ON v.code = p.vend_code AND v.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (sku, tenant_id) DO NOTHING;

-- ── 6. Product-category assignments ──────────────────────────
INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('BEV-001', 'Nước giải khát'),
    ('BEV-002', 'Nước giải khát'),
    ('BEV-003', 'Nước giải khát'),
    ('BEV-006', 'Nước suối / Nước tinh khiết'),
    ('BEV-007', 'Nước suối / Nước tinh khiết'),
    ('BEV-008', 'Bia & Nước có cồn'),
    ('BEV-009', 'Bia & Nước có cồn'),
    ('BEV-010', 'Bia & Nước có cồn'),
    ('BEV-011', 'Trà & Cà phê đóng gói'),
    ('BEV-012', 'Trà & Cà phê đóng gói'),
    ('BEV-004', 'Nước tăng lực'),
    ('BEV-005', 'Nước tăng lực'),
    ('BEV-013', 'Nước tăng lực'),
    ('FOOD-001', 'Mì gói & Cháo gói'),
    ('FOOD-002', 'Mì gói & Cháo gói'),
    ('FOOD-003', 'Mì gói & Cháo gói'),
    ('FOOD-004', 'Mì gói & Cháo gói'),
    ('FOOD-005', 'Sữa & Sản phẩm sữa'),
    ('FOOD-006', 'Sữa & Sản phẩm sữa'),
    ('FOOD-007', 'Gia vị & Nước chấm'),
    ('FOOD-008', 'Gia vị & Nước chấm'),
    ('FOOD-009', 'Gia vị & Nước chấm'),
    ('CONV-001', 'Bánh quy & Bánh ngọt'),
    ('CONV-002', 'Snack khoai tây'),
    ('CONV-003', 'Kẹo & Socola'),
    ('CONV-004', 'Bánh quy & Bánh ngọt'),
    ('BEAU-001', 'Vệ sinh cá nhân'),
    ('BEAU-002', 'Vệ sinh cá nhân'),
    ('BEAU-003', 'Vệ sinh cá nhân'),
    ('BEAU-004', 'Vệ sinh cá nhân'),
    ('CONV-005', 'Đồ gia dụng'),
    ('CONV-006', 'Đồ gia dụng'),
    ('CONV-007', 'Đồ gia dụng'),
    ('CONV-008', 'Thuốc lá'),
    ('CONV-009', 'Thuốc lá')
) AS pc(sku, cat_name)
JOIN product p ON p.sku = pc.sku AND p.tenant_id = current_setting('app.current_tenant', true)
JOIN category c ON c.name = pc.cat_name AND c.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT DO NOTHING;

-- ── 7. Inventory (initial stock) ─────────────────────────────
INSERT INTO inventory
    (tenant_id, product_id, quantity_in_stock, reorder_level, reorder_quantity,
     unit_cost, warehouse_location, status, inventory_type, last_restock_date)
SELECT
    current_setting('app.current_tenant', true),
    p.id,
    v.qty::INT, v.reorder_lvl::INT, v.reorder_qty::INT,
    v.unit_cost::NUMERIC, v.location, 'ACTIVE', 'RETAIL', NOW()
FROM (VALUES
    ('BEV-001', 120, 24,  96,  8500, 'Kho chính'),
    ('BEV-002', 120, 24,  96,  8500, 'Kho chính'),
    ('BEV-003', 100, 24,  96,  7500, 'Kho chính'),
    ('BEV-004',  60, 12,  48,  9500, 'Kho chính'),
    ('BEV-005',  80, 24,  96,  7000, 'Kho chính'),
    ('BEV-006', 200, 48, 144,  3500, 'Kho chính'),
    ('BEV-007', 200, 48, 144,  4500, 'Kho chính'),
    ('BEV-008',  72, 24,  72, 13000, 'Tủ lạnh'),
    ('BEV-009',  48, 12,  48, 18000, 'Tủ lạnh'),
    ('BEV-010',  72, 24,  72, 11000, 'Tủ lạnh'),
    ('BEV-011', 100, 24,  96,  8000, 'Kho chính'),
    ('BEV-012', 100, 24,  96,  8000, 'Kho chính'),
    ('BEV-013',  80, 24,  96,  7000, 'Kho chính'),
    ('FOOD-001', 200, 50, 150,  4800, 'Kho chính'),
    ('FOOD-002', 200, 50, 150,  4000, 'Kho chính'),
    ('FOOD-003', 150, 30, 100,  5500, 'Kho chính'),
    ('FOOD-004', 100, 30,  80,  8500, 'Kho chính'),
    ('FOOD-005',  60, 24,  48,  5800, 'Tủ lạnh'),
    ('FOOD-006',  60, 24,  48,  5200, 'Tủ lạnh'),
    ('FOOD-007',  50, 12,  36, 12500, 'Kho chính'),
    ('FOOD-008',  30, 10,  30, 31000, 'Kho chính'),
    ('FOOD-009',  80, 20,  60,  8500, 'Kho chính'),
    ('CONV-001',  80, 20,  60, 13000, 'Kho chính'),
    ('CONV-002', 100, 24,  72,  8500, 'Kho chính'),
    ('CONV-003',  60, 15,  45, 18000, 'Kho chính'),
    ('CONV-004',  50, 12,  36, 15000, 'Kho chính'),
    ('BEAU-001',  40, 10,  30, 25000, 'Kệ A-01'),
    ('BEAU-002',  30, 10,  30, 33000, 'Kệ A-01'),
    ('BEAU-003',  60, 15,  45, 13000, 'Kệ A-01'),
    ('BEAU-004',  40, 10,  30, 18000, 'Kệ A-01'),
    ('CONV-005',  40, 10,  30, 16000, 'Kệ B-01'),
    ('CONV-006',  30,  8,  24, 24000, 'Kệ B-01'),
    ('CONV-007', 100, 20,  60,  3000, 'Kệ B-02'),
    ('CONV-008',  50, 10,  30, 25000, 'Quầy tính tiền'),
    ('CONV-009',  30,  6,  18, 30000, 'Quầy tính tiền')
) AS v(sku, qty, reorder_lvl, reorder_qty, unit_cost, location)
JOIN product p ON p.sku = v.sku AND p.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (product_id) DO NOTHING;

-- ── 8. Loyalty program ───────────────────────────────────────
INSERT INTO loyalty_programs
    (tenant_id, points_per_amount, amount_per_points, redemption_points_per_discount,
     redemption_discount_amount, min_redemption_points, is_active)
VALUES
    (current_setting('app.current_tenant', true), 1, 10000, 100, 10000.00, 100, TRUE);

-- ── 9. Loyalty tiers ──────────────────────────────────────────
INSERT INTO loyalty_tiers
    (tenant_id, name, min_spend, points_multiplier, color, description, sort_order)
VALUES
    (current_setting('app.current_tenant', true), 'Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    (current_setting('app.current_tenant', true), 'Bạc',       2000000,   1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    (current_setting('app.current_tenant', true), 'Vàng',      10000000,  1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    (current_setting('app.current_tenant', true), 'Kim cương', 50000000,  2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND', 4);

-- ── 10. Default print template ────────────────────────────────
INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default) VALUES
    (current_setting('app.current_tenant', true), 'RECEIPT', 'Mặc định', '{
  "paperSize": "80mm",
  "showLogo": false,
  "showAddress": true,
  "showPhone": true,
  "showTaxCode": false,
  "showQrCode": false,
  "fontSize": 12,
  "lineSpacing": 1.2,
  "headerLines": ["{{shopName}}", "{{address}}", "ĐT: {{phone}}"],
  "footerLines": ["Cảm ơn quý khách!", "Hẹn gặp lại!"],
  "showOrderNumber": true,
  "showCashier": true,
  "showPaymentMethod": true,
  "showChangeAmount": true,
  "itemColumns": ["name", "qty", "price", "total"]
}', TRUE)
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

-- ── 11. Attribute groups & definitions (CONVENIENCE type) ─────
INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'basic_info', 'Thông tin cơ bản', 1
FROM product_type WHERE code = 'CONVENIENCE' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_group (tenant_id, product_type_id, code, name, display_order)
SELECT current_setting('app.current_tenant', true), id, 'storage_handling', 'Bảo quản & Xử lý', 2
FROM product_type WHERE code = 'CONVENIENCE' AND tenant_id = current_setting('app.current_tenant', true);

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'brand', 'Thương hiệu / Nhà sản xuất', 'STRING', FALSE, TRUE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'item_category', 'Danh mục hàng hóa', 'STRING', TRUE, TRUE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'package_size', 'Dung tích / Trọng lượng', 'STRING', FALSE, TRUE, TRUE, 3
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 4
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'barcode_upc', 'Mã vạch / Barcode', 'STRING', FALSE, TRUE, FALSE, 5
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'expiry_date', 'Hạn sử dụng', 'DATE', FALSE, FALSE, TRUE, 1
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO attribute_definition
    (tenant_id, product_type_id, attribute_group_id, code, name, data_type,
     required, searchable, filterable, display_order)
SELECT current_setting('app.current_tenant', true), pt.id, ag.id, 'storage_requirement', 'Điều kiện bảo quản', 'STRING', FALSE, FALSE, TRUE, 2
FROM product_type pt
JOIN attribute_group ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE' AND pt.tenant_id = current_setting('app.current_tenant', true)
ON CONFLICT (code, product_type_id) DO UPDATE SET name = EXCLUDED.name;

-- ── Shop configuration ────────────────────────────────────────
INSERT INTO shop_config (tenant_id, config_key, config_value, config_group, encrypted) VALUES
    (current_setting('app.current_tenant', true), 'cash_denominations', '1000,2000,5000,10000,20000,50000,100000,200000,500000', 'POS', FALSE)
ON CONFLICT (config_key, tenant_id) DO NOTHING;
