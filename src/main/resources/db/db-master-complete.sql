-- ============================================================
-- MASTER DATABASE - COMPLETE SETUP
-- Database: retail-platform-master
-- Covers: tenant registry, global users/roles/features
-- Run this once to bootstrap the master database from scratch.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `retail-platform`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `retail-platform`;

-- ──────────────────────────────────────────────────────────────
-- SECTION 1: TABLE DEFINITIONS
-- ──────────────────────────────────────────────────────────────

-- 1.1 features
CREATE TABLE IF NOT EXISTS `features` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Feature code/identifier (e.g. DASHBOARD, ORDER)',
    `display_name` VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Display name for UI',
    `description`  VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `active`       TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`   TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`   TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name`    (`name`),
    KEY `idx_name`          (`name`),
    KEY `idx_active`        (`active`),
    KEY `idx_deleted`       (`deleted`),
    KEY `idx_deleted_at`    (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.2 roles
CREATE TABLE IF NOT EXISTS `roles` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `description` VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`  TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`  TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name`    (`name`),
    KEY `idx_name`          (`name`),
    KEY `idx_deleted`       (`deleted`),
    KEY `idx_deleted_at`    (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.3 users (master-level super admins and vendor admins)
CREATE TABLE IF NOT EXISTS `users` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
    `username`                 VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `email`                    VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `password`                 VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `require_action`           VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `full_name`                VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `active`                   TINYINT(1)   NOT NULL DEFAULT 1,
    `account_non_locked`       TINYINT(1)   NOT NULL DEFAULT 1,
    `credentials_non_expired`  TINYINT(1)   NOT NULL DEFAULT 1,
    `account_non_expired`      TINYINT(1)   NOT NULL DEFAULT 1,
    `notes`                    VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `avatar`                   LONGTEXT              COLLATE utf8mb4_unicode_ci COMMENT 'Base64 encoded avatar',
    `color_preference`         VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `lang`                     VARCHAR(10)           COLLATE utf8mb4_unicode_ci DEFAULT 'vi',
    `failed_login_attempts`    INT          NOT NULL DEFAULT 0 COMMENT 'Reset to 0 on successful login',
    `created_at`               TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`               TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username`      (`username`),
    KEY `idx_username`            (`username`),
    KEY `idx_active`              (`active`),
    KEY `idx_users_active`        (`active`, `username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.4 employees (master-level; different structure from tenant employees)
CREATE TABLE IF NOT EXISTS `employees` (
    `id`          BIGINT                                                          NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100)  COLLATE utf8mb4_unicode_ci                        NOT NULL,
    `phone`       VARCHAR(20)   COLLATE utf8mb4_unicode_ci                        NOT NULL,
    `email`       VARCHAR(100)  COLLATE utf8mb4_unicode_ci                        DEFAULT NULL,
    `position`    VARCHAR(50)   COLLATE utf8mb4_unicode_ci                        NOT NULL,
    `user_id`     BIGINT                                                          DEFAULT NULL,
    `hire_date`   DATE                                                            DEFAULT NULL,
    `status`      ENUM('ACTIVE','INACTIVE','ON_LEAVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
    `description` TEXT          COLLATE utf8mb4_unicode_ci,
    `base_salary` DECIMAL(10,2)                                                   NOT NULL DEFAULT 0.00,
    `total_earned` DECIMAL(10,2)                                                  DEFAULT 0.00,
    `created_at`  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`  TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status`      (`status`),
    KEY `idx_phone`       (`phone`),
    KEY `idx_deleted_at`  (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.5 refresh_tokens
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `token`       VARCHAR(500) NOT NULL COLLATE utf8mb4_unicode_ci,
    `expiry_date` BIGINT       NOT NULL,
    `active`      TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`  BIGINT       NOT NULL,
    `updated_at`  BIGINT       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token`  (`token`),
    KEY `idx_token`        (`token`),
    KEY `idx_user_id`      (`user_id`),
    KEY `idx_active`       (`active`),
    CONSTRAINT `fk_rt_master_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.6 role_features
CREATE TABLE IF NOT EXISTS `role_features` (
    `id`         BIGINT    NOT NULL AUTO_INCREMENT,
    `role_id`    BIGINT    NOT NULL COMMENT 'Reference to roles table',
    `feature_id` BIGINT    NOT NULL COMMENT 'Reference to features table',
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_feature`  (`role_id`, `feature_id`),
    KEY `idx_role_id`             (`role_id`),
    KEY `idx_feature_id`          (`feature_id`),
    KEY `idx_created_at`          (`created_at`),
    CONSTRAINT `fk_rf_master_role`    FOREIGN KEY (`role_id`)    REFERENCES `roles`    (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rf_master_feature` FOREIGN KEY (`feature_id`) REFERENCES `features` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Maps roles to features for role-based access control';

-- 1.7 user_roles
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_ur_master_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_master_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.8 tenants
CREATE TABLE IF NOT EXISTS `tenants` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`             VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Short slug used in URL and X-Tenant-ID header',
    `name`                  VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `db_name`               VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'MySQL database name for this tenant',
    `active`                TINYINT(1)   NOT NULL DEFAULT 1,
    `expiration_date`       DATE                  DEFAULT NULL,
    `max_users`             INT                   DEFAULT NULL,
    `features`              TEXT                  COLLATE utf8mb4_unicode_ci COMMENT 'Comma-separated feature codes enabled for this tenant',
    `subscription_type`     VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_person_name`   VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_person_phone`  VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_person_email`  VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_person_zalo_id` VARCHAR(50)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`            BIGINT       NOT NULL,
    `updated_at`            BIGINT       NOT NULL,
    `active_at`             BIGINT                DEFAULT NULL,
    `active_by`             VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_by`            VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_by`            VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`),
    UNIQUE KEY `uk_db_name`   (`db_name`),
    KEY `idx_tenant_id`       (`tenant_id`),
    KEY `idx_active`          (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.9 active_sessions (single-device enforcement for master users)
CREATE TABLE IF NOT EXISTS `active_sessions` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `session_id`  VARCHAR(36)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'UUID embedded in JWT',
    `ip_address`  VARCHAR(45)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IPv4 or IPv6',
    `user_agent`  VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `login_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_active` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_sessions_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='One active session per master user. Replaced on force-login.';

-- ──────────────────────────────────────────────────────────────
-- SECTION 2: DEFAULT DATA
-- ──────────────────────────────────────────────────────────────

-- 2.1 Features (global feature registry)
-- Core features (applicable to all shop types)
INSERT IGNORE INTO `features` (`id`, `name`, `display_name`, `description`, `active`, `created_at`, `updated_at`, `deleted`, `deleted_at`) VALUES
(202601001, 'DASHBOARD',   'Bảng Điều Khiển',        'Xem tổng quan và thống kê chính của cửa hàng',                          1, NOW(), NOW(), 0, NULL),
(202601002, 'ORDER',       'Đơn Hàng',                'Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',             1, NOW(), NOW(), 0, NULL),
(202601003, 'MY_WORK',     'Công Việc Của Tôi',       'Xem công việc được giao cho nhân viên hiện tại',                        1, NOW(), NOW(), 0, NULL),
(202601004, 'PRODUCT',     'Sản Phẩm & Dịch Vụ',     'Quản lý danh sách sản phẩm, dịch vụ, giá cả và hoa hồng',              1, NOW(), NOW(), 0, NULL),
(202601005, 'PROMOTION',   'Khuyến Mãi',              'Tạo và quản lý các chương trình khuyến mãi, giảm giá',                 1, NOW(), NOW(), 0, NULL),
(202601006, 'EMPLOYEE',    'Nhân Viên',               'Quản lý nhân viên, chức vụ, lương cơ bản',                             1, NOW(), NOW(), 0, NULL),
(202601007, 'SALARY',      'Lương Nhân Viên',         'Quản lý bảng lương, tính toán lương, chi trả',                         1, NOW(), NOW(), 0, NULL),
(202601008, 'CUSTOMER',    'Khách Hàng',              'Quản lý thông tin khách hàng, lịch sử mua hàng',                       1, NOW(), NOW(), 0, NULL),
(202601009, 'INVOICE',     'Hóa Đơn',                 'Quản lý hóa đơn, xuất hóa đơn điện tử',                                1, NOW(), NOW(), 0, NULL),
(202601010, 'REVENUE',     'Doanh Thu',               'Xem báo cáo doanh thu, lợi nhuận, chi phí',                            1, NOW(), NOW(), 0, NULL),
(202601011, 'USER',        'Người Dùng',              'Quản lý tài khoản người dùng, quyền truy cập',                         1, NOW(), NOW(), 0, NULL),
(202601012, 'SHOP_INFO',   'Thông Tin Cửa Hàng',      'Cập nhật thông tin cửa hàng, cấu hình hệ thống',                       1, NOW(), NOW(), 0, NULL),
-- Master-only features
(202601013, 'TENANT_MGMT', 'Quản Lý Cửa Hàng',       'Quản lý các chi nhánh, cửa hàng trong hệ thống',                       1, NOW(), NOW(), 0, NULL),
(202601014, 'VENDOR',      'Nhà Cung Cấp',            'Quản lý nhà cung cấp và đơn đặt hàng nhập',                            1, NOW(), NOW(), 0, NULL),
(202601015, 'VENDOR_MGMT', 'Quản Lý Nhà Phân Phối',  'Super admin quản lý nhà phân phối và giao shop cho nhà phân phối',     1, NOW(), NOW(), 0, NULL),
(202601016, 'INVENTORY',   'Quản Lý Kho',             'Theo dõi mức tồn kho, quản lý cấp phát, lịch sử kho',                  1, NOW(), NOW(), 0, NULL),
(202601017, 'LOYALTY',     'Chương Trình Loyalty',    'Quản lý chương trình khách hàng thân thiết, điểm thưởng',              1, NOW(), NOW(), 0, NULL);

-- 2.2 Roles
INSERT IGNORE INTO `roles` (`id`, `name`, `description`, `created_at`, `updated_at`, `deleted`, `deleted_at`) VALUES
(202600001, 'MASTER_TENANT', 'Quản trị hệ thống - Toàn quyền quản lý tenant và người dùng master',            NOW(), NOW(), 0, NULL),
(202600002, 'VENDOR_ADMIN',  'Quản trị nhà phân phối - Quản lý danh sách shop thuộc nhà phân phối',           NOW(), NOW(), 0, NULL);

-- 2.3 Users
-- Default password: CHANGE THIS before going to production.
-- The hash below corresponds to a bcrypt-encoded password.
INSERT IGNORE INTO `users` (`id`, `username`, `email`, `password`, `full_name`, `active`, `account_non_locked`, `credentials_non_expired`, `account_non_expired`, `lang`, `failed_login_attempts`, `deleted`) VALUES
(1, 'Administrator', 'admin@retailplatform.local',
 '$2a$10$pyg6ud.T6WmFBtcsyBp2TujecrqKNifJZPmewv2aJDApOVZWxbbi6',
 'System Administrator', 1, 1, 1, 1, 'vi', 0, 0);

-- 2.4 Role-feature mappings
-- MASTER_TENANT: manages users, tenants, and vendor relationships
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`) VALUES
(202600001, 202601011, NOW()),   -- USER
(202600001, 202601013, NOW()),   -- TENANT_MGMT
(202600001, 202601015, NOW());   -- VENDOR_MGMT

-- VENDOR_ADMIN: reads their own vendor + tenant list
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`) VALUES
(202600002, 202601015, NOW());   -- VENDOR_MGMT

-- 2.5 User-role assignments
INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`) VALUES
(1, 202600001);  -- Administrator → MASTER_TENANT

-- ──────────────────────────────────────────────────────────────
-- SECTION 3: SAMPLE TENANT RECORD (convenience store)
-- Uncomment and adjust before provisioning a new store.
-- ──────────────────────────────────────────────────────────────

-- INSERT IGNORE INTO `tenants`
--     (`tenant_id`, `name`, `db_name`, `active`, `expiration_date`, `max_users`,
--      `features`, `subscription_type`,
--      `contact_person_name`, `contact_person_phone`, `contact_person_email`,
--      `created_at`, `updated_at`, `created_by`)
-- VALUES
--     ('taphoamau', 'Tạp Hóa Bình Dân', 'retail-platform-taphoamau', 1,
--      DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 10,
--      'DASHBOARD,ORDER,PRODUCT,CUSTOMER,PROMOTION,EMPLOYEE,SALARY,INVOICE,REVENUE,USER,SHOP_INFO,VENDOR,INVENTORY,LOYALTY',
--      'STANDARD',
--      'Nguyễn Văn A', '0901234567', 'owner@taphoabinhdan.vn',
--      UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'Administrator');

SET FOREIGN_KEY_CHECKS = 1;
