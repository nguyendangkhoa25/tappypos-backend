-- ============================================================
-- TENANT DATABASE — DEFAULT DATA: CONVENIENCE STORE (TẠP HÓA)
-- Database: retail-platform-{tenantId}
-- Run AFTER ddl.sql. All statements are INSERT IGNORE / ON DUPLICATE KEY UPDATE — safe to re-run.
-- Admin user and walk-in customer are seeded by TenantProvisioningService (not here).
-- ============================================================

SET NAMES utf8mb4;

-- Features, roles, and role-feature mappings are seeded by TenantProvisioningService.provision()
-- based on the master admin's selection at shop creation time. Do not duplicate them here.

-- ── 1. Shop info ──────────────────────────────────────────────
INSERT INTO `shop_info` (`shop_name`, `address`, `company_name`, `phone`)
VALUES ('Tạp Hóa Bình Dân', '123 Đường Nguyễn Trãi, Phường 3, Quận 5, TP.HCM', 'Hộ kinh doanh Tạp Hóa Bình Dân', '0901234567');

-- ── 2. Product types (18 standard types) ─────────────────────
INSERT INTO `product_type` (`code`, `name`, `description`) VALUES
    ('FOOD',         'Thực phẩm',                  'Thực phẩm và đồ ăn'),
    ('BEVERAGE',     'Đồ uống',                    'Nước giải khát, bia, nước suối'),
    ('DRUG',         'Dược phẩm',                  'Thuốc và sản phẩm dược'),
    ('CONVENIENCE',  'Hàng tiêu dùng',             'Hàng tiêu dùng thiết yếu'),
    ('BIKE',         'Xe đạp / Xe máy',            'Xe đạp và phụ tùng xe máy'),
    ('HARDWARE',     'Đồ sắt / Dụng cụ',          'Đồ sắt và dụng cụ'),
    ('CLOTHING',     'Quần áo / May mặc',          'Quần áo và phụ kiện'),
    ('ELECTRONICS',  'Điện tử',                    'Thiết bị điện tử'),
    ('FURNITURE',    'Đồ nội thất',                'Nội thất gia đình'),
    ('BEAUTY',       'Làm đẹp / Chăm sóc cá nhân','Sản phẩm làm đẹp và vệ sinh cá nhân'),
    ('TOYS',         'Đồ chơi / Trò chơi',        'Đồ chơi và trò chơi'),
    ('BOOKS',        'Sách / Văn phòng phẩm',     'Sách và văn phòng phẩm'),
    ('SPORTS',       'Thể thao / Ngoài trời',     'Thiết bị thể thao'),
    ('AUTO_PARTS',   'Phụ tùng ô tô',             'Phụ tùng và phụ kiện ô tô'),
    ('APPLIANCES',   'Đồ gia dụng',                'Thiết bị gia dụng'),
    ('OFFICE',       'Văn phòng phẩm',             'Đồ dùng văn phòng'),
    ('PET',          'Thú cưng',                  'Thức ăn và phụ kiện thú cưng'),
    ('HEALTH',       'Sức khỏe / Dinh dưỡng',     'Sản phẩm sức khỏe và dinh dưỡng')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 3. Categories ─────────────────────────────────────────────
INSERT INTO `category` (`id`, `name`, `parent_id`) VALUES
    -- Top-level
    (1,  'Đồ uống',                     NULL),
    (2,  'Thực phẩm',                   NULL),
    (3,  'Bánh kẹo & Snacks',           NULL),
    (4,  'Vệ sinh cá nhân',             NULL),
    (5,  'Đồ gia dụng',                 NULL),
    (6,  'Thuốc lá',                    NULL),
    (7,  'Sữa & Sản phẩm sữa',         NULL),
    (8,  'Gia vị & Nước chấm',          NULL),
    -- Sub-categories under Đồ uống
    (9,  'Nước giải khát',              1),
    (10, 'Nước suối / Nước tinh khiết', 1),
    (11, 'Bia & Nước có cồn',           1),
    (12, 'Trà & Cà phê đóng gói',       1),
    (13, 'Nước tăng lực',               1),
    -- Sub-categories under Thực phẩm
    (14, 'Mì gói & Cháo gói',           2),
    (15, 'Thực phẩm khô',               2),
    -- Sub-categories under Bánh kẹo
    (16, 'Bánh quy & Bánh ngọt',        3),
    (17, 'Kẹo & Socola',                3),
    (18, 'Snack khoai tây',             3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 4. Walk-in customer ───────────────────────────────────────
INSERT IGNORE INTO `customers`
    (`id`, `name`, `phone`, `email`, `notes`, `deleted`)
VALUES
    (790000001, 'Khách lẻ', '0000000000', NULL,
     'Khách hàng lẻ - không có thông tin liên hệ', FALSE);

-- ── 5. Vendors ───────────────────────────────────────────────
INSERT IGNORE INTO `vendors`
    (`id`, `name`, `code`, `contact_name`, `phone`, `payment_terms`, `is_active`, `deleted`)
VALUES
    (1, 'Nhà cung cấp đồ uống & FMCG', 'VND-001', NULL, NULL, 'NET_30', 1, 0),
    (2, 'Nhà cung cấp thực phẩm & tiêu dùng', 'VND-002', NULL, NULL, 'NET_30', 1, 0);

-- ── 6. Products (35 common convenience store items) ──────────
INSERT INTO `product`
    (`id`, `product_type_id`, `sku`, `name`, `description`,
     `price`, `cost_price`, `unit`, `vendor_id`, `status`)
VALUES
-- Beverages (BEVERAGE)
(1,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-001', 'Coca-Cola 330ml',              'Nước ngọt có ga lon 330ml',               12000,  8500, 'lon',   1, 'ACTIVE'),
(2,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-002', 'Pepsi 330ml',                  'Nước ngọt có ga lon 330ml',               12000,  8500, 'lon',   1, 'ACTIVE'),
(3,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-003', '7-Up 330ml',                   'Nước ngọt có ga lon 330ml',               11000,  7500, 'lon',   1, 'ACTIVE'),
(4,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-004', 'Red Bull 250ml',               'Nước tăng lực lon 250ml',                 13000,  9500, 'lon',   1, 'ACTIVE'),
(5,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-005', 'Number One 330ml',             'Nước tăng lực lon 330ml',                 10000,  7000, 'lon',   1, 'ACTIVE'),
(6,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-006', 'Nước suối Aqua 500ml',         'Nước tinh khiết chai 500ml',               6000,  3500, 'chai',  1, 'ACTIVE'),
(7,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-007', 'Nước suối La Vie 500ml',       'Nước khoáng thiên nhiên chai 500ml',       7000,  4500, 'chai',  1, 'ACTIVE'),
(8,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-008', 'Bia Tiger 330ml',              'Bia lon 330ml',                           18000, 13000, 'lon',   1, 'ACTIVE'),
(9,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-009', 'Bia Heineken 330ml',           'Bia lon 330ml',                           25000, 18000, 'lon',   1, 'ACTIVE'),
(10, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-010', 'Bia Saigon Đỏ 330ml',         'Bia lon 330ml',                           15000, 11000, 'lon',   1, 'ACTIVE'),
(11, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-011', 'Trà Olong Tea Plus 455ml',     'Trà Olong chai PET 455ml',                12000,  8000, 'chai',  1, 'ACTIVE'),
(12, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-012', 'Trà xanh 0 Độ 455ml',         'Trà xanh không đường chai 455ml',         12000,  8000, 'chai',  1, 'ACTIVE'),
(13, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-013', 'Sting Dâu 330ml',             'Nước tăng lực hương dâu lon 330ml',       10000,  7000, 'lon',   1, 'ACTIVE'),
-- Food (FOOD)
(14, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-001', 'Mì Hảo Hảo Tôm Chua Cay 75g',    'Mì ăn liền gói 75g',                      7000,  4800, 'gói',   1, 'ACTIVE'),
(15, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-002', 'Mì 3 Miền Bò Hầm Rau 65g',        'Mì ăn liền gói 65g',                      6000,  4000, 'gói',   1, 'ACTIVE'),
(16, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-003', 'Phở Bò Ăn Liền Vifon 65g',        'Phở ăn liền gói 65g',                     8000,  5500, 'gói',   1, 'ACTIVE'),
(17, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-004', 'Cháo Thịt Bằm Vifon 50g',         'Cháo ăn liền gói 50g',                   12000,  8500, 'gói',   1, 'ACTIVE'),
(18, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-005', 'Sữa TH True Milk 180ml',          'Sữa tươi tiệt trùng hộp',                 8000,  5800, 'hộp',   2, 'ACTIVE'),
(19, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-006', 'Sữa Vinamilk UHT 180ml',          'Sữa tươi UHT hộp 180ml',                  7500,  5200, 'hộp',   2, 'ACTIVE'),
(20, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-007', 'Nước mắm Nam Ngư 500ml',          'Nước mắm chai thuỷ tinh 500ml',           18000, 12500, 'chai',  2, 'ACTIVE'),
(21, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-008', 'Dầu ăn Neptune 500ml',            'Dầu thực vật chai 500ml',                 42000, 31000, 'chai',  2, 'ACTIVE'),
(22, (SELECT id FROM product_type WHERE code='FOOD'), 'FOOD-009', 'Mì chính Ajinomoto 100g',         'Bột ngọt gói 100g',                       12000,  8500, 'gói',   2, 'ACTIVE'),
-- Snacks (CONVENIENCE)
(23, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-001', 'Bánh Oreo 97g',              'Bánh quy kem 97g',                       18000, 13000, 'gói',   2, 'ACTIVE'),
(24, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-002', 'Snack Poca Khoai Tây BBQ 68g','Snack khoai tây vị BBQ gói 68g',        12000,  8500, 'gói',   2, 'ACTIVE'),
(25, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-003', 'Kẹo Dừa Bến Tre 200g',       'Kẹo dừa truyền thống gói 200g',          25000, 18000, 'gói',   2, 'ACTIVE'),
(26, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-004', 'Bánh Kinh Đô Hương Vani',    'Bánh quy bơ hộp 150g',                   22000, 15000, 'hộp',   2, 'ACTIVE'),
-- Personal care (BEAUTY)
(27, (SELECT id FROM product_type WHERE code='BEAUTY'), 'BEAU-001', 'Kem đánh răng Colgate 150g',     'Kem đánh răng bảo vệ toàn diện',          35000, 25000, 'tuýp',  1, 'ACTIVE'),
(28, (SELECT id FROM product_type WHERE code='BEAUTY'), 'BEAU-002', 'Dầu gội Clear Mát Lạnh 170ml',   'Dầu gội sạch gàu chai 170ml',             45000, 33000, 'chai',  1, 'ACTIVE'),
(29, (SELECT id FROM product_type WHERE code='BEAUTY'), 'BEAU-003', 'Xà phòng Lifebuoy 90g',          'Xà phòng kháng khuẩn 90g',                18000, 13000, 'bánh',  1, 'ACTIVE'),
(30, (SELECT id FROM product_type WHERE code='BEAUTY'), 'BEAU-004', 'Bàn chải Oral-B Classic',        'Bàn chải đánh răng lông mềm',             25000, 18000, 'cái',   1, 'ACTIVE'),
-- Household (CONVENIENCE)
(31, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-005', 'Nước rửa chén Sunlight Chanh 500ml','Nước rửa chén chai 500ml',         22000, 16000, 'chai',  2, 'ACTIVE'),
(32, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-006', 'Bột giặt Omo Comfort 400g',  'Bột giặt thơm gói 400g',                 32000, 24000, 'gói',   2, 'ACTIVE'),
(33, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-007', 'Túi nilon HDPE 30 cái',      'Túi đựng hàng 30x40cm, 30 cái/cuộn',     5000,  3000, 'cuộn',  2, 'ACTIVE'),
-- Tobacco (CONVENIENCE)
(34, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-008', 'Thuốc lá Esse Menthol',      'Thuốc lá Esse bạc hà 1 bao',             30000, 25000, 'bao',   2, 'ACTIVE'),
(35, (SELECT id FROM product_type WHERE code='CONVENIENCE'), 'CONV-009', 'Thuốc lá 555 State Express', 'Thuốc lá 555 1 bao',                      35000, 30000, 'bao',   2, 'ACTIVE');

-- ── 7. Product-category assignments ─────────────────────────
INSERT IGNORE INTO `product_category` (`product_id`, `category_id`) VALUES
    -- Beverages → sub-categories
    (1,9),(2,9),(3,9),                   -- Nước giải khát
    (6,10),(7,10),                       -- Nước suối
    (8,11),(9,11),(10,11),               -- Bia
    (11,12),(12,12),                     -- Trà
    (4,13),(5,13),(13,13),              -- Nước tăng lực
    -- Food
    (14,14),(15,14),(16,14),(17,14),    -- Mì gói / Cháo gói
    (18,7),(19,7),                      -- Sữa
    (20,8),(21,8),(22,8),               -- Gia vị
    -- Snacks
    (23,16),(24,18),(25,17),(26,16),    -- Bánh kẹo / Snacks
    -- Personal care
    (27,4),(28,4),(29,4),(30,4),
    -- Household
    (31,5),(32,5),(33,5),
    -- Tobacco
    (34,6),(35,6);

-- ── 8. Inventory (initial stock) ────────────────────────────
INSERT INTO `inventory`
    (`product_id`, `quantity_in_stock`, `reorder_level`, `reorder_quantity`,
     `unit_cost`, `warehouse_location`, `status`, `inventory_type`, `last_restock_date`)
VALUES
    -- Beverages
    (1,  120, 24,  96,  8500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (2,  120, 24,  96,  8500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (3,  100, 24,  96,  7500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (4,   60, 12,  48,  9500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (5,   80, 24,  96,  7000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (6,  200, 48, 144,  3500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (7,  200, 48, 144,  4500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (8,   72, 24,  72, 13000, 'Tủ lạnh',         'ACTIVE', 'RETAIL', NOW()),
    (9,   48, 12,  48, 18000, 'Tủ lạnh',         'ACTIVE', 'RETAIL', NOW()),
    (10,  72, 24,  72, 11000, 'Tủ lạnh',         'ACTIVE', 'RETAIL', NOW()),
    (11, 100, 24,  96,  8000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (12, 100, 24,  96,  8000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (13,  80, 24,  96,  7000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    -- Food
    (14, 200, 50, 150,  4800, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (15, 200, 50, 150,  4000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (16, 150, 30, 100,  5500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (17, 100, 30,  80,  8500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (18,  60, 24,  48,  5800, 'Tủ lạnh',         'ACTIVE', 'RETAIL', NOW()),
    (19,  60, 24,  48,  5200, 'Tủ lạnh',         'ACTIVE', 'RETAIL', NOW()),
    (20,  50, 12,  36, 12500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (21,  30, 10,  30, 31000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (22,  80, 20,  60,  8500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    -- Snacks
    (23,  80, 20,  60, 13000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (24, 100, 24,  72,  8500, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (25,  60, 15,  45, 18000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    (26,  50, 12,  36, 15000, 'Kho chính',       'ACTIVE', 'RETAIL', NOW()),
    -- Personal care
    (27,  40, 10,  30, 25000, 'Kệ A-01',         'ACTIVE', 'RETAIL', NOW()),
    (28,  30, 10,  30, 33000, 'Kệ A-01',         'ACTIVE', 'RETAIL', NOW()),
    (29,  60, 15,  45, 13000, 'Kệ A-01',         'ACTIVE', 'RETAIL', NOW()),
    (30,  40, 10,  30, 18000, 'Kệ A-01',         'ACTIVE', 'RETAIL', NOW()),
    -- Household
    (31,  40, 10,  30, 16000, 'Kệ B-01',         'ACTIVE', 'RETAIL', NOW()),
    (32,  30,  8,  24, 24000, 'Kệ B-01',         'ACTIVE', 'RETAIL', NOW()),
    (33, 100, 20,  60,  3000, 'Kệ B-02',         'ACTIVE', 'RETAIL', NOW()),
    -- Tobacco
    (34,  50, 10,  30, 25000, 'Quầy tính tiền',  'ACTIVE', 'RETAIL', NOW()),
    (35,  30,  6,  18, 30000, 'Quầy tính tiền',  'ACTIVE', 'RETAIL', NOW());

-- ── 9. Loyalty program ───────────────────────────────────────
INSERT INTO `loyalty_programs`
    (`points_per_amount`, `amount_per_points`, `redemption_points_per_discount`,
     `redemption_discount_amount`, `min_redemption_points`, `is_active`)
VALUES
    (1, 10000, 100, 10000.00, 100, 1);

-- ── 10. Loyalty tiers ─────────────────────────────────────────
INSERT INTO `loyalty_tiers`
    (`name`, `min_spend`, `points_multiplier`, `color`, `description`, `sort_order`)
VALUES
    ('Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    ('Bạc',       2000000,   1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    ('Vàng',      10000000,  1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    ('Kim cương', 50000000,  2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND', 4);

-- ── 14. Default print template ────────────────────────────────
INSERT IGNORE INTO `print_templates` (`template_type`, `name`, `config_json`, `is_default`) VALUES
    ('RECEIPT', 'Mặc định', '{
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
}', 1);

-- ── 15. Attribute groups & definitions (CONVENIENCE type) ─────
INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'basic_info', 'Thông tin cơ bản', 1 FROM `product_type` WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 1;

INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'storage_handling', 'Bảo quản & Xử lý', 2 FROM `product_type` WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 2;

INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'supplier_info', 'Thông tin nhà cung cấp', 3 FROM `product_type` WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 3;

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'brand', 'Thương hiệu / Nhà sản xuất', 'STRING', FALSE, TRUE, TRUE, 1
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'item_category', 'Danh mục hàng hóa', 'STRING', TRUE, TRUE, TRUE, 2
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'package_size', 'Dung tích / Trọng lượng', 'STRING', FALSE, TRUE, TRUE, 3
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 4
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'barcode_upc', 'Mã vạch / Barcode', 'STRING', FALSE, TRUE, FALSE, 5
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'expiry_date', 'Hạn sử dụng', 'DATE', FALSE, FALSE, TRUE, 1
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition`
    (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`,
     `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'storage_requirement', 'Điều kiện bảo quản', 'STRING', FALSE, FALSE, TRUE, 2
FROM `product_type` pt
JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 16. Banks (Vietnamese bank reference list) ────────────────
INSERT IGNORE INTO `banks` (`code`, `bin`, `name`, `short_name`, `sort_order`) VALUES
-- State-owned / state-capital banks
('VCB',   '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam',                              'Vietcombank',          1),
('CTG',   '970415', 'Ngân hàng TMCP Công thương Việt Nam',                               'VietinBank',           2),
('BID',   '970418', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam',                      'BIDV',                 3),
('AGR',   '970405', 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam',            'Agribank',             4),
-- Top private joint-stock banks
('MBB',   '970422', 'Ngân hàng TMCP Quân đội',                                           'MB Bank',              5),
('TCB',   '970407', 'Ngân hàng TMCP Kỹ thương Việt Nam',                                 'Techcombank',          6),
('VPB',   '970432', 'Ngân hàng TMCP Việt Nam Thịnh Vượng',                               'VPBank',               7),
('ACB',   '970416', 'Ngân hàng TMCP Á Châu',                                             'ACB',                  8),
('STB',   '970403', 'Ngân hàng TMCP Sài Gòn Thương Tín',                                 'Sacombank',            9),
('TPB',   '970423', 'Ngân hàng TMCP Tiên Phong',                                         'TPBank',              10),
('HDB',   '970437', 'Ngân hàng TMCP Phát triển TP.HCM',                                  'HDBank',              11),
('VIB',   '970441', 'Ngân hàng TMCP Quốc tế Việt Nam',                                   'VIB',                 12),
('SHB',   '970443', 'Ngân hàng TMCP Sài Gòn - Hà Nội',                                  'SHB',                 13),
('EIB',   '970431', 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam',                            'Eximbank',            14),
('LPB',   '970449', 'Ngân hàng TMCP Bưu điện Liên Việt',                                 'LienVietPostBank',    15),
('MSB',   '970426', 'Ngân hàng TMCP Hàng Hải Việt Nam',                                  'MSB',                 16),
('OCB',   '970448', 'Ngân hàng TMCP Phương Đông',                                        'OCB',                 17),
('SSB',   '970440', 'Ngân hàng TMCP Đông Nam Á',                                         'SeABank',             18),
('ABB',   '970425', 'Ngân hàng TMCP An Bình',                                             'ABBank',              19),
-- Smaller private banks
('BAB',   '970409', 'Ngân hàng TMCP Bắc Á',                                              'BacABank',            20),
('BVB',   '970454', 'Ngân hàng TMCP Bản Việt',                                           'BVBank',              21),
('KLB',   '970462', 'Ngân hàng TMCP Kiên Long',                                          'KienLongBank',        22),
('NAB',   '970428', 'Ngân hàng TMCP Nam Á',                                              'NamABank',            23),
('NCB',   '970419', 'Ngân hàng TMCP Quốc Dân',                                           'NCB',                 24),
('PGB',   '970430', 'Ngân hàng TMCP Xăng dầu Petrolimex',                                'PGBank',              25),
('PVCB',  '970452', 'Ngân hàng TMCP Đại Chúng Việt Nam',                                 'PVcomBank',           26),
('SGB',   '970400', 'Ngân hàng TMCP Sài Gòn Công Thương',                                'Saigonbank',          27),
('VBB',   '970433', 'Ngân hàng TMCP Việt Nam Thương Tín',                                'VietBank',            28),
('BVK',   '970438', 'Ngân hàng TMCP Bảo Việt',                                           'BaoVietBank',         29),
('GPB',   '970408', 'Ngân hàng TMCP Dầu khí Toàn Cầu',                                   'GPBank',              30),
('OJB',   '970414', 'Ngân hàng Thương mại TNHH MTV Đại Dương',                           'OceanBank',           31),
('DAB',   '970406', 'Ngân hàng TMCP Đông Á',                                             'DongABank',           32),
('CBB',   '970444', 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam',                   'CBBank',              33),
-- Digital / neobanks
('CAKE',  '546034', 'CAKE by VPBank',                                                     'CAKE',                40),
('UBANK', '546035', 'Ubank by VPBank',                                                    'Ubank',               41),
('TIMO',  '963388', 'Timo by BVBank',                                                     'Timo',                42),
('TNEX',  '533948', 'TNEX by MSB',                                                        'TNEX',                43),
-- Foreign banks
('HSBC',  '458761', 'Ngân hàng TNHH MTV HSBC Việt Nam',                                  'HSBC Vietnam',        50),
('SC',    '970410', 'Ngân hàng TNHH MTV Standard Chartered Việt Nam',                     'Standard Chartered',  51),
('SHIN',  '970424', 'Ngân hàng TNHH MTV Shinhan Việt Nam',                                'Shinhan Vietnam',     52),
('WOORI', '970457', 'Ngân hàng TNHH MTV Woori Việt Nam',                                  'Woori Vietnam',       53),
('UOB',   '970458', 'Ngân hàng UOB Việt Nam',                                             'UOB Vietnam',         54),
('HLB',   '970442', 'Ngân hàng TNHH MTV Hong Leong Việt Nam',                             'Hong Leong Vietnam',  55),
('PBB',   '970439', 'Ngân hàng TNHH MTV Public Việt Nam',                                 'Public Bank Vietnam', 56),
('IBK',   '970455', 'Ngân hàng Công nghiệp Hàn Quốc - IBK Việt Nam',                     'IBK Vietnam',         57),
('KEXIM', '668888', 'Ngân hàng Xuất nhập khẩu Hàn Quốc - KEXIM Việt Nam',               'KEXIM Vietnam',       58);

-- Ensure SHOP_OWNER has all active features (mirrors V047 migration).
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`)
SELECT r.id, f.id
FROM `roles` r
CROSS JOIN `features` f
WHERE r.name = 'SHOP_OWNER'
  AND f.active = 1;
