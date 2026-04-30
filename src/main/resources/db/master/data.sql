-- ============================================================
-- MASTER DATABASE — DEFAULT DATA
-- Database: retail-platform-master
-- Run AFTER ddl.sql. All statements use INSERT IGNORE — safe to re-run.
-- ============================================================

SET NAMES utf8mb4;

-- ── 1. Features ───────────────────────────────────────────────
-- Complete list of all platform feature flags.
-- These IDs are stable and referenced in JWT payloads and role_features.
INSERT IGNORE INTO `features`
    (`id`, `name`, `display_name`, `description`, `active`, `created_at`, `updated_at`, `deleted`, `deleted_at`)
VALUES
    -- Core shop management
    (202601001, 'DASHBOARD',    'Bảng Điều Khiển',        'Xem tổng quan và thống kê chính của cửa hàng',                          1, NOW(), NOW(), 0, NULL),
    (202601002, 'ORDER',        'Đơn Hàng',                'Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',             1, NOW(), NOW(), 0, NULL),
    (202601003, 'MY_WORK',      'Công Việc Của Tôi',       'Xem công việc được giao cho nhân viên hiện tại',                        1, NOW(), NOW(), 0, NULL),
    (202601004, 'PRODUCT',      'Sản Phẩm & Dịch Vụ',     'Quản lý danh sách sản phẩm, dịch vụ, giá cả',                          1, NOW(), NOW(), 0, NULL),
    (202601005, 'PROMOTION',    'Khuyến Mãi',              'Tạo và quản lý các chương trình khuyến mãi, giảm giá',                  1, NOW(), NOW(), 0, NULL),
    (202601006, 'EMPLOYEE',     'Nhân Viên',               'Quản lý nhân viên, chức vụ, lương cơ bản',                             1, NOW(), NOW(), 0, NULL),
    (202601007, 'SALARY',       'Lương Nhân Viên',         'Quản lý bảng lương, tính toán lương, chi trả',                         1, NOW(), NOW(), 0, NULL),
    (202601008, 'CUSTOMER',     'Khách Hàng',              'Quản lý thông tin khách hàng, lịch sử mua hàng, tích điểm',            1, NOW(), NOW(), 0, NULL),
    (202601009, 'INVOICE',      'Hóa Đơn',                 'Quản lý hóa đơn, xuất hóa đơn điện tử',                               1, NOW(), NOW(), 0, NULL),
    (202601010, 'REVENUE',      'Doanh Thu',               'Xem báo cáo doanh thu, lợi nhuận, chi phí',                            1, NOW(), NOW(), 0, NULL),
    (202601011, 'USER',         'Người Dùng',              'Quản lý tài khoản người dùng, quyền truy cập',                         1, NOW(), NOW(), 0, NULL),
    (202601012, 'SHOP_INFO',    'Thông Tin Cửa Hàng',      'Cập nhật thông tin cửa hàng, cấu hình hệ thống',                      1, NOW(), NOW(), 0, NULL),
    -- Master / system management
    (202601013, 'TENANT_MGMT', 'Quản Lý Cửa Hàng',        'Tạo, kích hoạt và quản lý các cửa hàng trong hệ thống',               1, NOW(), NOW(), 0, NULL),
    -- Supply chain
    (202601014, 'VENDOR',       'Nhà Cung Cấp',            'Quản lý nhà cung cấp và đơn đặt hàng nhập',                           1, NOW(), NOW(), 0, NULL),
    (202601015, 'VENDOR_MGMT',  'Nhà Phân Phối',  'Super admin quản lý nhà phân phối và giao shop',                       1, NOW(), NOW(), 0, NULL),
    -- Operations
    (202601016, 'INVENTORY',    'Quản Lý Kho',             'Quản lý tồn kho, nhập xuất kho và kiểm kho',                          1, NOW(), NOW(), 0, NULL),
    (202601017, 'POS',          'Điểm Bán Hàng',           'Bán hàng tại quầy, thanh toán và in hóa đơn',                         1, NOW(), NOW(), 0, NULL),
    (202601018, 'ACTIVITY_LOG', 'Nhật Ký Hoạt Động',       'Xem nhật ký hoạt động của người dùng trong cửa hàng',                 1, NOW(), NOW(), 0, NULL),
    (202601019, 'PAWN',         'Cầm Đồ',                  'Quản lý hợp đồng cầm đồ, lãi suất và thanh lý tài sản',               1, NOW(), NOW(), 0, NULL),
    -- Master / system management (cont.)
    (202601020, 'FEEDBACK_MGMT',    'Quản Lý Phản Hồi',       'Xem và xử lý phản hồi, góp ý từ người dùng toàn hệ thống',            1, NOW(), NOW(), 0, NULL),
    (202601021, 'MASTER_DASHBOARD', 'Bảng Điều Khiển Hệ Thống','Xem tổng quan và thống kê của hệ thống master',                       1, NOW(), NOW(), 0, NULL),
    -- Granular shop features (decoupled from CUSTOMER / REVENUE)
    (202601022, 'LOYALTY',          'Tích Điểm Khách Hàng',   'Chương trình tích điểm và phần thưởng khách hàng',                     1, NOW(), NOW(), 0, NULL),
    (202601023, 'EXPENSE',          'Chi Phí',                 'Theo dõi và quản lý chi phí hoạt động cửa hàng',                      1, NOW(), NOW(), 0, NULL),
    -- Platform utility features (previously universal, now token-gated)
    (202601024, 'NOTIFICATION',     'Thông Báo',               'Nhận thông báo và nhắc nhở từ hệ thống',                               1, NOW(), NOW(), 0, NULL),
    (202601025, 'FEEDBACK',         'Góp Ý',                   'Gửi phản hồi và đề xuất đến quản trị hệ thống',                       1, NOW(), NOW(), 0, NULL),
    -- Granular SHOP_INFO / INVOICE sub-features
    (202601026, 'PRINT_TEMPLATE',   'Mẫu In',                  'Quản lý mẫu in biên nhận và hóa đơn',                                 1, NOW(), NOW(), 0, NULL),
    (202601027, 'BANK_ACCOUNT',     'Tài Khoản Ngân Hàng',     'Quản lý tài khoản ngân hàng của cửa hàng',                           1, NOW(), NOW(), 0, NULL),
    (202601028, 'ACCOUNTING',       'Kế Toán',                  'Xem báo cáo kế toán tổng hợp',                                       1, NOW(), NOW(), 0, NULL);

-- ── 2. Roles ──────────────────────────────────────────────────
INSERT IGNORE INTO `roles`
    (`id`, `name`, `description`, `created_at`, `updated_at`, `deleted`, `deleted_at`)
VALUES
    (202600001, 'MASTER_TENANT', 'Quản trị hệ thống - Toàn quyền quản lý tenant và người dùng master', NOW(), NOW(), 0, NULL),
    (202600002, 'VENDOR_ADMIN',  'Quản trị nhà phân phối - Quản lý danh sách shop thuộc nhà phân phối', NOW(), NOW(), 0, NULL);

-- ── 3. Role-feature mappings ──────────────────────────────────
-- MASTER_TENANT: full platform management — master dashboard, users, tenants, vendors, feedback, activity logs
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`)
VALUES
    (202600001, 202601011, NOW()),   -- USER
    (202600001, 202601013, NOW()),   -- TENANT_MGMT
    (202600001, 202601015, NOW()),   -- VENDOR_MGMT
    (202600001, 202601018, NOW()),   -- ACTIVITY_LOG
    (202600001, 202601020, NOW()),   -- FEEDBACK_MGMT
    (202600001, 202601021, NOW());   -- MASTER_DASHBOARD

-- VENDOR_ADMIN: can view assigned tenants and master dashboard
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`)
VALUES
    (202600002, 202601013, NOW()),   -- TENANT_MGMT
    (202600002, 202601021, NOW());   -- MASTER_DASHBOARD

-- ── 4. Default admin user ─────────────────────────────────────
-- Password = '1234' (bcrypt, cost 10). CHANGE on first login.
INSERT IGNORE INTO `users`
    (`id`, `username`, `email`, `password`, `full_name`,
     `active`, `account_non_locked`, `credentials_non_expired`, `account_non_expired`,
     `failed_login_attempts`, `lang`, `deleted`)
VALUES
    (79260001, 'Administrator', 'nguyendangkhoa25@gmail.com',
     '$2a$10$u2vWhVe3r0JliVDtwrkU4eqEBUWmXPsZ4dyazsbuoco5gL6L2N8B.',
     'Quản Trị Viên',
     1, 1, 1, 1, 0, 'vi', 0);

INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`) VALUES (79260001, 202600001);