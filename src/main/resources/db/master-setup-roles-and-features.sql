-- ================================================================
-- Master DB: roles and feature-role setup
-- Run against: retail-platform-master database
-- Safe to re-run: all statements use INSERT IGNORE
-- ================================================================

SET NAMES utf8mb4;

-- ── 1. Features ──────────────────────────────────────────────────
--  Add the VENDOR feature (used by tenant SHOP_OWNER for purchase orders / vendor pages)
--  and VENDOR_MGMT (super admin manages vendors at master level)

INSERT IGNORE INTO `features`
    (`id`, `name`, `display_name`, `description`, `active`, `created_at`, `updated_at`, `deleted`, `deleted_at`)
VALUES
    (202601014, 'VENDOR',      'Nhà Cung Cấp',          'Quản lý nhà cung cấp và đơn đặt hàng nhập',                   1, NOW(), NOW(), 0, NULL),
    (202601015, 'VENDOR_MGMT', 'Quản Lý Nhà Phân Phối', 'Super admin quản lý nhà phân phối và giao shop cho nhà phân phối', 1, NOW(), NOW(), 0, NULL);


-- ── 2. Roles ─────────────────────────────────────────────────────
--  MASTER_TENANT (202600001) already exists — INSERT IGNORE skips it.
--  Add VENDOR_ADMIN: vendor admin users live in master DB (no tenant context),
--  so this role must be seeded here.

INSERT IGNORE INTO `roles`
    (`id`, `name`, `description`, `created_at`, `updated_at`, `deleted`, `deleted_at`)
VALUES
    (202600001, 'MASTER_TENANT', 'Quản trị hệ thống - Toàn quyền quản lý tenant và người dùng master', NOW(), NOW(), 0, NULL),
    (202600002, 'VENDOR_ADMIN',  'Quản trị nhà phân phối - Quản lý danh sách shop thuộc nhà phân phối', NOW(), NOW(), 0, NULL);


-- ── 3. Feature-role mappings ──────────────────────────────────────
--
--  MASTER_TENANT (super admin):
--    • USER        — manage master-DB users
--    • TENANT_MGMT — create/activate/deactivate shops
--    • VENDOR_MGMT — create vendors and assign shops to vendors
--
--  VENDOR_ADMIN:
--    • VENDOR_MGMT — read their own vendor + tenant list (endpoint scoped by vendorId in JWT)

-- MASTER_TENANT existing rows (INSERT IGNORE keeps them)
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`)
VALUES
    (202600001, 202601011, NOW()),   -- USER
    (202600001, 202601013, NOW()),   -- TENANT_MGMT
    (202600001, 202601015, NOW());   -- VENDOR_MGMT

-- VENDOR_ADMIN
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`)
VALUES
    (202600002, 202601015, NOW());   -- VENDOR_MGMT
