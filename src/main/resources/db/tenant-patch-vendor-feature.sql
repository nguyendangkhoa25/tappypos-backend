-- ================================================================
-- Tenant DB: add VENDOR feature and fix SHOP_OWNER role gaps
-- Run against: each retail-platform-{tenantId} database
-- Safe to re-run: all statements use INSERT IGNORE
-- ================================================================

SET NAMES utf8mb4;

-- ── 1. Add VENDOR feature ────────────────────────────────────────
--  Used by VendorsPage and PurchaseOrdersPage (featureName="VENDOR" in TenantRoutes)

INSERT IGNORE INTO `features`
    (`id`, `name`, `display_name`, `description`, `active`, `created_at`, `updated_at`, `deleted`, `deleted_at`)
VALUES
    (202601013, 'TENANT_MGMT', 'Quản Lý Cửa Hàng',    'Quản lý các chi nhánh, cửa hàng trong hệ thống',     1, NOW(), NOW(), 0, NULL),
    (202601014, 'VENDOR',      'Nhà Cung Cấp',          'Quản lý nhà cung cấp và đơn đặt hàng nhập',           1, NOW(), NOW(), 0, NULL);


-- ── 2. Role-feature gaps in SHOP_OWNER ──────────────────────────
--  SHOP_OWNER (role id=1) was missing MY_WORK and VENDOR

INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`)
VALUES
    (1, 202601003, NOW()),   -- MY_WORK  (was missing for SHOP_OWNER)
    (1, 202601014, NOW());   -- VENDOR


-- ── 3. Role-feature gaps in MANAGER ─────────────────────────────
--  MANAGER (role id=2) was missing DASHBOARD and SALARY

INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`)
VALUES
    (2, 202601001, NOW()),   -- DASHBOARD (was missing for MANAGER)
    (2, 202601007, NOW());   -- SALARY    (was missing for MANAGER)
