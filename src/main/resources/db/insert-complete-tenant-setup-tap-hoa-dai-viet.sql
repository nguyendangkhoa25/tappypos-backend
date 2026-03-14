-- Retail Platform - Complete Tenant Setup with User & Role
-- Tenant: Tap Hoa Dai Viet (Convenience Store)
-- Date: 2026-03-17

SET NAMES utf8mb4;

-- ============================================================================
-- STEP 1: Create Convenience Store Tenant, run master db
-- ============================================================================
INSERT INTO `tenants` (
  `tenant_id`,
  `name`,
  `db_name`,
  `active`,
  `expiration_date`,
  `max_users`,
  `features`,
  `subscription_type`,
  `contact_person_name`,
  `contact_person_phone`,
  `contact_person_email`,
  `contact_person_zalo_id`,
  `created_at`,
  `updated_at`,
  `created_by`,
  `updated_by`
) VALUES (
  'tap-hoa-dai-viet',
  'Tạp Hóa Đại Việt',
  'tap-hoa-dai-viet',
  1,
  '2027-03-17',
  1,
  'DASHBOARD,ORDER,MY_WORK,PRODUCT,PROMOTION,EMPLOYEE,SALARY,CUSTOMER,INVOICE,REVENUE,USER,SHOP_INFO,TENANT_MGMT,POS,INVENTORY,SUPPLIER,ANALYTICS,LOYALTY,BARCODE,SETTING,ACCESSIBILITY,CONVN_EXPIRY,CONVN_LOWSTOCK,CONVN_MULTILOC',
  'STANDARD',
  'Nguyễn Văn A',
  '+84912345678',
  'daiviet@example.com',
  'zalo_daiviet_123',
  1742246400000,
  1742246400000,
  'Administrator',
  'Administrator'
);

-- ============================================================================
-- STEP 1b: Insert Shop Information
-- ============================================================================
INSERT INTO `shop_info` (
  `id`,
  `shop_name`,
  `address`,
  `company_name`,
  `default_tax_rate`,
  `e_invoice_username`,
  `e_invoice_password`,
  `e_invoice_key`,
  `phone`,
  `email`,
  `supplier_tax_code`,
  `invoice_vendor`,
  `website`,
  `created_at`,
  `updated_at`,
  `deleted`,
  `deleted_at`,
  `template_code`,
  `invoice_system`,
  `invoice_series`
) VALUES (
  3920260101,
  'Tạp Hóa Đại Việt',
  'Số 123, Đường Lê Lợi, Phường Bến Thành, Quận 1, Thành Phố Hồ Chí Minh',
  'Công Ty TNHH Tạp Hóa Đại Việt',
  0.00,
  'taproicdaiviet',
  '',
  '',
  '+84912345678',
  'taproicdaiviet@example.com',
  '',
  'VIETTEL',
  '',
  '2026-03-17 00:00:00',
  '2026-03-17 00:00:00',
  0,
  NULL,
  '2026/01',
  'S-INVOICE',
  'CM26THDV');

-- ============================================================================
-- STEP 2: Create Convenience Store Roles with Correct Descriptions
-- ============================================================================
INSERT INTO `roles` (
  `id`,
  `name`,
  `description`,
  `created_at`,
  `updated_at`,
  `deleted`,
  `deleted_at`
) VALUES
  (202601101, 'SHOP_OWNER', 'Shop Owner - Full access to all store features and management', '2026-03-17 00:00:00', '2026-03-17 00:00:00', 0, NULL),
  (202601102, 'MANAGER', 'Store Manager - Access to management, inventory, orders, staff, reports', '2026-03-17 00:00:00', '2026-03-17 00:00:00', 0, NULL),
  (202601103, 'CASHIER', 'Cashier/Receptionist - POS transactions, customer service, order processing', '2026-03-17 00:00:00', '2026-03-17 00:00:00', 0, NULL),
  (202601104, 'CLEANER', 'Cleaner/Staff - Limited access to basic store information only', '2026-03-17 00:00:00', '2026-03-17 00:00:00', 0, NULL),
  (202601105, 'ACCOUNTANT', 'Accountant - Finance, salary, revenue, analytics access', '2026-03-17 00:00:00', '2026-03-17 00:00:00', 0, NULL);

-- ============================================================================
-- STEP 3: Insert Administrator User
-- ============================================================================
INSERT INTO `users` (
  `username`,
  `email`,
  `password`,
  `full_name`,
  `active`,
  `account_non_locked`,
  `credentials_non_expired`,
  `account_non_expired`,
  `notes`,
  `created_at`,
  `updated_at`,
  `deleted`,
  `lang`
) VALUES (
  'admin_daiviet',
  'admin@taproicdaiviet.local',
  '$2a$10$pyg6ud.T6WmFBtcsyBp2TujecrqKNifJZPmewv2aJDApOVZWxbbi6',
  'Quản Lý Tạp Hóa Đại Việt',
  1,
  1,
  1,
  1,
  'Shop owner account for Tạp Hóa Đại Việt convenience store',
  '2026-03-17 00:00:00',
  '2026-03-17 00:00:00',
  0,
  'vi'
);

-- ============================================================================
-- STEP 4: Assign Features to SHOP_OWNER (All 24 features)
-- ============================================================================
INSERT INTO `role_features` (role_id, feature_id, created_at) VALUES
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601001, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601002, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601003, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601004, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601005, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601006, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601007, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601008, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601009, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601010, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601011, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601012, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601013, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601014, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601015, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601016, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601017, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601018, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601019, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601020, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202601021, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202602001, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202602002, '2026-03-17 00:00:00'),
((SELECT id FROM roles WHERE name = 'SHOP_OWNER'), 202602003, '2026-03-17 00:00:00');

-- ============================================================================
-- STEP 5: Assign Features to MANAGER (16 features)
-- ============================================================================
INSERT INTO `role_features` (role_id, feature_id, created_at) VALUES
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601001, '2026-03-17 00:00:00'), -- DASHBOARD
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601002, '2026-03-17 00:00:00'), -- ORDER
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601003, '2026-03-17 00:00:00'), -- MY_WORK
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601004, '2026-03-17 00:00:00'), -- PRODUCT
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601005, '2026-03-17 00:00:00'), -- PROMOTION
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601006, '2026-03-17 00:00:00'), -- EMPLOYEE
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601008, '2026-03-17 00:00:00'), -- CUSTOMER
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601009, '2026-03-17 00:00:00'), -- INVOICE
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601010, '2026-03-17 00:00:00'), -- REVENUE
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601015, '2026-03-17 00:00:00'), -- INVENTORY
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601016, '2026-03-17 00:00:00'), -- SUPPLIER
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601017, '2026-03-17 00:00:00'), -- ANALYTICS
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601018, '2026-03-17 00:00:00'), -- LOYALTY
((SELECT id FROM roles WHERE name = 'MANAGER'), 202601019, '2026-03-17 00:00:00'), -- BARCODE
((SELECT id FROM roles WHERE name = 'MANAGER'), 202602001, '2026-03-17 00:00:00'), -- CONVN_EXPIRY
((SELECT id FROM roles WHERE name = 'MANAGER'), 202602002, '2026-03-17 00:00:00'); -- CONVN_LOWSTOCK

-- ============================================================================
-- STEP 6: Assign Features to CASHIER (6 features)
-- ============================================================================
INSERT INTO `role_features` (role_id, feature_id, created_at) VALUES
((SELECT id FROM roles WHERE name = 'CASHIER'), 202601002, '2026-03-17 00:00:00'), -- ORDER
((SELECT id FROM roles WHERE name = 'CASHIER'), 202601003, '2026-03-17 00:00:00'), -- MY_WORK
((SELECT id FROM roles WHERE name = 'CASHIER'), 202601008, '2026-03-17 00:00:00'), -- CUSTOMER
((SELECT id FROM roles WHERE name = 'CASHIER'), 202601009, '2026-03-17 00:00:00'), -- INVOICE
((SELECT id FROM roles WHERE name = 'CASHIER'), 202601014, '2026-03-17 00:00:00'), -- POS
((SELECT id FROM roles WHERE name = 'CASHIER'), 202601019, '2026-03-17 00:00:00'); -- BARCODE

-- ============================================================================
-- STEP 7: Assign Features to CLEANER (1 feature)
-- ============================================================================
INSERT INTO `role_features` (role_id, feature_id, created_at) VALUES
((SELECT id FROM roles WHERE name = 'CLEANER'), 202601012, '2026-03-17 00:00:00'); -- SHOP_INFO

-- ============================================================================
-- STEP 8: Assign Features to ACCOUNTANT (5 features)
-- ============================================================================
INSERT INTO `role_features` (role_id, feature_id, created_at) VALUES
((SELECT id FROM roles WHERE name = 'ACCOUNTANT'), 202601007, '2026-03-17 00:00:00'), -- SALARY
((SELECT id FROM roles WHERE name = 'ACCOUNTANT'), 202601009, '2026-03-17 00:00:00'), -- INVOICE
((SELECT id FROM roles WHERE name = 'ACCOUNTANT'), 202601010, '2026-03-17 00:00:00'), -- REVENUE
((SELECT id FROM roles WHERE name = 'ACCOUNTANT'), 202601017, '2026-03-17 00:00:00'), -- ANALYTICS
((SELECT id FROM roles WHERE name = 'ACCOUNTANT'), 202601001, '2026-03-17 00:00:00'); -- DASHBOARD

-- ============================================================================
-- STEP 9: Assign User to SHOP_OWNER Role
-- ============================================================================
INSERT INTO `user_roles` (user_id, role_id) VALUES (
  (SELECT id FROM users WHERE username = 'admin_daiviet'),
  (SELECT id FROM roles WHERE name = 'SHOP_OWNER')
);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these queries to verify the complete setup:

-- Check tenant was created:
-- SELECT id, tenant_id, name, db_name, active, max_users, subscription_type FROM tenants WHERE tenant_id = 'tap-hoa-dai-viet';

-- Check all roles created:
-- SELECT id, name, description FROM roles WHERE id IN (202601101, 202601102, 202601103, 202601104, 202601105);

-- Check role feature counts:
-- SELECT r.name, COUNT(rf.feature_id) as feature_count
-- FROM roles r
-- LEFT JOIN role_features rf ON r.id = rf.role_id
-- WHERE r.id IN (202601101, 202601102, 202601103, 202601104, 202601105)
-- GROUP BY r.id, r.name
-- ORDER BY r.id;

-- Check user was created:
-- SELECT id, username, email, full_name, active FROM users WHERE username = 'admin_daiviet';

-- Check user role assignment:
-- SELECT u.username, r.name FROM user_roles ur
-- JOIN users u ON ur.user_id = u.id
-- JOIN roles r ON ur.role_id = r.id
-- WHERE u.username = 'admin_daiviet';

-- ============================================================================
-- SETUP SUMMARY
-- ============================================================================
-- Tenant: Tạp Hóa Đại Việt (tap-hoa-dai-viet)
-- Status: ACTIVE | Subscription: STANDARD | Max Users: 1
-- Expiration: 2027-03-17 | Features: 24 (all core + convenience store specific)
--
-- Roles Created: 5
--   - 202601101: SHOP_OWNER (24 features)
--   - 202601102: MANAGER (16 features)
--   - 202601103: CASHIER (6 features)
--   - 202601104: CLEANER (1 feature)
--   - 202601105: ACCOUNTANT (5 features)
--
-- User Created: 1
--   - Username: admin_daiviet
--   - Email: admin@taproicdaiviet.local
--   - Password: 123456 (hashed: $2a$10$pyg6ud.T6WmFBtcsyBp2TujecrqKNifJZPmewv2aJDApOVZWxbbi6)
--   - Full Name: Quản Lý Tạp Hóa Đại Việt
--   - Role: SHOP_OWNER
--
-- ============================================================================
