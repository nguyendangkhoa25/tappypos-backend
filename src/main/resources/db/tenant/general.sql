-- ============================================================
-- TENANT DATABASE — DEFAULT DATA: GENERAL (ALL OTHER SHOP TYPES)
-- Used for: JEWELRY, PHARMACY, ELECTRONICS, FOOD_BEVERAGE, FASHION,
--           BARBER_SHOP, COFFEE_SHOP, RESTAURANT, OTHER
-- Database: retail-platform-{tenantId}
-- Run AFTER ddl.sql. All statements are INSERT IGNORE / ON DUPLICATE KEY UPDATE — safe to re-run.
-- Admin user and walk-in customer are seeded by TenantProvisioningService (not here).
-- ============================================================

SET NAMES utf8mb4;

-- Features, roles, and role-feature mappings are seeded by TenantProvisioningService.provision()
-- based on the master admin's selection at shop creation time. Do not duplicate them here.

-- ── 1. Shop info ──────────────────────────────────────────────
INSERT INTO `shop_info` (`shop_name`) VALUES ('Cửa Hàng Của Tôi');

-- ── 2. Product types (all 18 standard types) ─────────────────
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

-- ── 3. Walk-in customer ───────────────────────────────────────
INSERT IGNORE INTO `customers`
    (`id`, `name`, `phone`, `email`, `notes`, `deleted`)
VALUES
    (1, 'Khách lẻ', '0000000000', NULL,
     'Khách hàng lẻ - không có thông tin liên hệ', FALSE);

-- ── 4. Loyalty program ────────────────────────────────────────
INSERT INTO `loyalty_programs`
    (`points_per_amount`, `amount_per_points`, `redemption_points_per_discount`,
     `redemption_discount_amount`, `min_redemption_points`, `is_active`)
VALUES
    (1, 10000, 100, 10000.00, 100, 1);

-- ── 5. Loyalty tiers ──────────────────────────────────────────
INSERT INTO `loyalty_tiers`
    (`name`, `min_spend`, `points_multiplier`, `color`, `description`, `sort_order`)
VALUES
    ('Đồng',      0,         1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    ('Bạc',       2000000,   1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    ('Vàng',      10000000,  1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    ('Kim cương', 50000000,  2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND', 4);

-- ── 6. Default print template ─────────────────────────────────
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

-- ── 7. Banks (Vietnamese bank reference list) ────────────────
INSERT IGNORE INTO `banks` (`code`, `bin`, `name`, `short_name`, `sort_order`) VALUES
('VCB',   '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam',                              'Vietcombank',          1),
('CTG',   '970415', 'Ngân hàng TMCP Công thương Việt Nam',                               'VietinBank',           2),
('BID',   '970418', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam',                      'BIDV',                 3),
('AGR',   '970405', 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam',            'Agribank',             4),
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
('BAB',   '970409', 'Ngân hàng TMCP Bắc Á',                                              'BacABank',            20),
('BVB',   '970454', 'Ngân hàng TMCP Bản Việt',                                           'BVBank',              21),
('KLB',   '970462', 'Ngân hàng TMCP Kiên Long',                                          'KienLongBank',        22),
('NAB',   '970428', 'Ngân hàng TMCP Nam Á',                                              'NamABank',            23),
('NCB',   '970419', 'Ngân hàng TMCP Quốc Dân',                                           'NCB',                 24),
('HSBC',  '458761', 'Ngân hàng TNHH MTV HSBC Việt Nam',                                  'HSBC Vietnam',        50),
('SC',    '970410', 'Ngân hàng TNHH MTV Standard Chartered Việt Nam',                     'Standard Chartered',  51),
('SHIN',  '970424', 'Ngân hàng TNHH MTV Shinhan Việt Nam',                                'Shinhan Vietnam',     52),
('WOORI', '970457', 'Ngân hàng TNHH MTV Woori Việt Nam',                                  'Woori Vietnam',       53),
('UOB',   '970458', 'Ngân hàng UOB Việt Nam',                                             'UOB Vietnam',         54);

-- Ensure SHOP_OWNER has all active features (mirrors V047 migration).
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`)
SELECT r.id, f.id
FROM `roles` r
CROSS JOIN `features` f
WHERE r.name = 'SHOP_OWNER'
  AND f.active = 1;
