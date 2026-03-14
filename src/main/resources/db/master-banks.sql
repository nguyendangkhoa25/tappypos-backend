-- Banks reference table for master database
-- Run this script against retail-platform-master database

CREATE TABLE IF NOT EXISTS `banks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Short bank code (e.g., VCB, TCB)',
  `bin` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'VietQR BIN code for QR generation and logo display',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Full official bank name in Vietnamese',
  `short_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Common short name used in daily transactions',
  `sort_order` int NOT NULL DEFAULT '999' COMMENT 'Display order (lower = shown first)',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_code` (`code`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_active` (`is_active`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default list of all banks in Vietnam with VietQR BIN codes
INSERT INTO `banks` (`code`, `bin`, `name`, `short_name`, `sort_order`) VALUES
-- Big four state-owned / state-capital banks
('VCB',   '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam',                              'Vietcombank',       1),
('CTG',   '970415', 'Ngân hàng TMCP Công thương Việt Nam',                               'VietinBank',        2),
('BID',   '970418', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam',                      'BIDV',              3),
('AGR',   '970405', 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam',            'Agribank',          4),

-- Top private joint-stock banks
('MBB',   '970422', 'Ngân hàng TMCP Quân đội',                                           'MB Bank',           5),
('TCB',   '970407', 'Ngân hàng TMCP Kỹ thương Việt Nam',                                 'Techcombank',       6),
('VPB',   '970432', 'Ngân hàng TMCP Việt Nam Thịnh Vượng',                               'VPBank',            7),
('ACB',   '970416', 'Ngân hàng TMCP Á Châu',                                             'ACB',               8),
('STB',   '970403', 'Ngân hàng TMCP Sài Gòn Thương Tín',                                 'Sacombank',         9),
('TPB',   '970423', 'Ngân hàng TMCP Tiên Phong',                                         'TPBank',            10),
('HDB',   '970437', 'Ngân hàng TMCP Phát triển TP.HCM',                                  'HDBank',            11),
('VIB',   '970441', 'Ngân hàng TMCP Quốc tế Việt Nam',                                   'VIB',               12),
('SHB',   '970443', 'Ngân hàng TMCP Sài Gòn - Hà Nội',                                  'SHB',               13),
('EIB',   '970431', 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam',                            'Eximbank',          14),
('LPB',   '970449', 'Ngân hàng TMCP Bưu điện Liên Việt',                                 'LienVietPostBank',  15),
('MSB',   '970426', 'Ngân hàng TMCP Hàng Hải Việt Nam',                                  'MSB',               16),
('OCB',   '970448', 'Ngân hàng TMCP Phương Đông',                                        'OCB',               17),
('SSB',   '970440', 'Ngân hàng TMCP Đông Nam Á',                                         'SeABank',           18),
('ABB',   '970425', 'Ngân hàng TMCP An Bình',                                             'ABBank',            19),

-- Smaller private joint-stock banks
('BAB',   '970409', 'Ngân hàng TMCP Bắc Á',                                              'BacABank',          20),
('BVB',   '970454', 'Ngân hàng TMCP Bản Việt',                                           'BVBank',            21),
('KLB',   '970462', 'Ngân hàng TMCP Kiên Long',                                          'KienLongBank',      22),
('NAB',   '970428', 'Ngân hàng TMCP Nam Á',                                              'NamABank',          23),
('NCB',   '970419', 'Ngân hàng TMCP Quốc Dân',                                           'NCB',               24),
('PGB',   '970430', 'Ngân hàng TMCP Xăng dầu Petrolimex',                                'PGBank',            25),
('PVCB',  '970452', 'Ngân hàng TMCP Đại Chúng Việt Nam',                                 'PVcomBank',         26),
('SGB',   '970400', 'Ngân hàng TMCP Sài Gòn Công Thương',                                'Saigonbank',        27),
('VCCB',  '970454', 'Ngân hàng TMCP Bản Việt (Viet Capital Bank)',                        'VietCapitalBank',   28),
('VBB',   '970433', 'Ngân hàng TMCP Việt Nam Thương Tín',                                'VietBank',          29),
('BVK',   '970438', 'Ngân hàng TMCP Bảo Việt',                                           'BaoVietBank',       30),
('GPB',   '970408', 'Ngân hàng TMCP Dầu khí Toàn Cầu',                                   'GPBank',            31),
('OJB',   '970414', 'Ngân hàng Thương mại TNHH MTV Đại Dương',                           'OceanBank',         32),
('DAB',   '970406', 'Ngân hàng TMCP Đông Á',                                             'DongABank',         33),
('CBB',   '970444', 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam',                   'CBBank',            34),

-- Digital / neobanks
('CAKE',  '546034', 'CAKE by VPBank',                                                     'CAKE',              40),
('UBANK', '546035', 'Ubank by VPBank',                                                    'Ubank',             41),
('TIMO',  '963388', 'Timo by BVBank',                                                     'Timo',              42),
('TNEX',  '533948', 'TNEX by MSB',                                                        'TNEX',              43),

-- Foreign banks (branches / subsidiaries in Vietnam)
('HSBC',  '458761', 'Ngân hàng TNHH MTV HSBC Việt Nam',                                  'HSBC Vietnam',      50),
('SC',    '970410', 'Ngân hàng TNHH MTV Standard Chartered Việt Nam',                     'Standard Chartered',51),
('CITI',  '533948', 'Ngân hàng Citibank Việt Nam',                                        'Citibank Vietnam',  52),
('SHIN',  '970424', 'Ngân hàng TNHH MTV Shinhan Việt Nam',                                'Shinhan Vietnam',   53),
('WOORI', '970457', 'Ngân hàng TNHH MTV Woori Việt Nam',                                  'Woori Vietnam',     54),
('UOB',   '970458', 'Ngân hàng UOB Việt Nam',                                             'UOB Vietnam',       55),
('HLB',   '970442', 'Ngân hàng TNHH MTV Hong Leong Việt Nam',                             'Hong Leong Vietnam',56),
('PBB',   '970439', 'Ngân hàng TNHH MTV Public Việt Nam',                                 'Public Bank Vietnam',57),
('IBK',   '970455', 'Ngân hàng Công nghiệp Hàn Quốc - IBK Việt Nam',                     'IBK Vietnam',       58),
('KEXIM', '668888', 'Ngân hàng Xuất nhập khẩu Hàn Quốc - KEXIM Việt Nam',               'KEXIM Vietnam',     59);

-- If banks table already exists without the bin column, run:
-- ALTER TABLE `banks` ADD COLUMN `bin` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `code`;
-- UPDATE `banks` SET `bin` = '970436' WHERE `code` = 'VCB'; -- etc.
