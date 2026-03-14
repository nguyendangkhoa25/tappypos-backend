-- Fix Script: Add Missing Inventory Tables to tap-hoa-dai-viet Tenant Database
-- This script adds the inventory and inventory_movement tables that were missing
-- Date: 2026-03-21
--
-- HOW TO RUN:
-- 1. Execute this script directly against the tap-hoa-dai-viet database
--    mysql -u root -p tap-hoa-dai-viet < fix-missing-inventory-tables-tap-hoa-dai-viet.sql
-- 2. Or run the SQL commands manually in your MySQL client

USE `tap-hoa-dai-viet`;

-- ============================================================================
-- Create inventory table (if not exists)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `inventory` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `product_id` bigint NOT NULL,
    `quantity_in_stock` bigint NOT NULL DEFAULT 0,
    `reorder_level` bigint NOT NULL DEFAULT 10,
    `reorder_quantity` bigint NOT NULL DEFAULT 50,
    `unit_cost` decimal(15, 2) NOT NULL DEFAULT 0.00,
    `warehouse_location` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `last_restock_date` datetime,
    `expiry_date` date,
    `batch_number` varchar(100) COLLATE utf8mb4_unicode_ci,
    `notes` varchar(500) COLLATE utf8mb4_unicode_ci,
    `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, DISCONTINUED',
    `inventory_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RETAIL' COMMENT 'RETAIL, WHOLESALE, WAREHOUSE',
    `deleted` boolean NOT NULL DEFAULT FALSE,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` datetime,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_product_id` (`product_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_warehouse_location` (`warehouse_location`),
    INDEX `idx_status` (`status`),
    INDEX `idx_inventory_type` (`inventory_type`),
    INDEX `idx_expiry_date` (`expiry_date`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_low_stock` (`quantity_in_stock`, `reorder_level`, `deleted`),
    INDEX `idx_expired_items` (`expiry_date`, `deleted`),
    INDEX `idx_composite_search` (`deleted`, `status`, `inventory_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Create inventory_movement table (if not exists)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `inventory_movement` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `inventory_id` bigint NOT NULL,
    `movement_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IN, OUT, ADJUSTMENT, RETURN, DAMAGE, EXPIRED',
    `quantity` decimal(15, 2) NOT NULL,
    `reference_number` varchar(100) COLLATE utf8mb4_unicode_ci,
    `reference_type` varchar(50) COLLATE utf8mb4_unicode_ci,
    `created_by_user` varchar(100) COLLATE utf8mb4_unicode_ci,
    `reason` varchar(255) COLLATE utf8mb4_unicode_ci,
    `notes` varchar(500) COLLATE utf8mb4_unicode_ci,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` boolean NOT NULL DEFAULT FALSE,
    `deleted_at` datetime,
    PRIMARY KEY (`id`),
    KEY `idx_inventory_id` (`inventory_id`),
    KEY `idx_movement_type` (`movement_type`),
    KEY `idx_reference_number` (`reference_number`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted` (`deleted`),
    CONSTRAINT `fk_inventory_movement_inventory` FOREIGN KEY (`inventory_id`) REFERENCES `inventory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Verify tables were created
-- ============================================================================
SELECT 'Tables created successfully!' as status;
SHOW TABLES LIKE '%inventory%';

-- ============================================================================
-- Verification Query - Run these to verify the fix
-- ============================================================================
-- SELECT COUNT(*) FROM inventory;
-- SELECT COUNT(*) FROM inventory_movement;
-- DESCRIBE inventory;
-- DESCRIBE inventory_movement;

