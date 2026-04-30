-- ============================================================
-- TENANT DATABASE — DDL
-- Applies to every tenant database: retail-platform-{tenantId}
-- Consolidates all Flyway migrations V002–V042 into final state.
-- Run this on a fresh tenant database before inserting data.
-- All statements use IF NOT EXISTS — safe to re-run.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ── SECTION 1: Auth & Users ───────────────────────────────────

-- 1.1 features
-- Per-tenant feature flags (subset of master features granted by subscription).
CREATE TABLE IF NOT EXISTS `features` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `display_name` VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `description`  VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `active`       TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`   TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`   TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name`   (`name`),
    KEY `idx_name`         (`name`),
    KEY `idx_active`       (`active`),
    KEY `idx_deleted`      (`deleted`),
    KEY `idx_deleted_at`   (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.2 roles
-- Tenant-level roles (SHOP_OWNER, MANAGER, etc.). Vary by shop type.
CREATE TABLE IF NOT EXISTS `roles` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `description` VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`  TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`  TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name`   (`name`),
    KEY `idx_name`         (`name`),
    KEY `idx_deleted`      (`deleted`),
    KEY `idx_deleted_at`   (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.3 users (V001 + V032 adds failed_login_attempts)
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
    `failed_login_attempts`    INT          NOT NULL DEFAULT 0 COMMENT 'Reset to 0 on successful login; locked at 5',
    `created_at`               TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`               TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username`   (`username`),
    KEY `idx_username`         (`username`),
    KEY `idx_active`           (`active`),
    KEY `idx_users_active`     (`active`, `username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.4 refresh_tokens
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
    CONSTRAINT `fk_rt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.5 user_roles
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.6 role_features
CREATE TABLE IF NOT EXISTS `role_features` (
    `id`         BIGINT    NOT NULL AUTO_INCREMENT,
    `role_id`    BIGINT    NOT NULL,
    `feature_id` BIGINT    NOT NULL,
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_feature` (`role_id`, `feature_id`),
    KEY `idx_role_id`            (`role_id`),
    KEY `idx_feature_id`         (`feature_id`),
    KEY `idx_created_at`         (`created_at`),
    CONSTRAINT `fk_rf_role`    FOREIGN KEY (`role_id`)    REFERENCES `roles`    (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rf_feature` FOREIGN KEY (`feature_id`) REFERENCES `features` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Maps tenant roles to feature flags';

-- 1.7 active_sessions (V034) — single-device login enforcement
CREATE TABLE IF NOT EXISTS `active_sessions` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `session_id`  VARCHAR(36)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'UUID embedded in JWT',
    `ip_address`  VARCHAR(45)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_agent`  VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `login_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_active` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_sessions_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='One active session per user. Replaced on force-login.';

-- ── SECTION 2: Product System (EAV) ──────────────────────────

-- 2.1 product_type (V002)
CREATE TABLE IF NOT EXISTS `product_type` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `name`        VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `description` VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     BOOLEAN      NOT NULL DEFAULT FALSE,
    `deleted_at`  DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code`   (`code`),
    KEY `idx_code`         (`code`),
    KEY `idx_deleted`      (`deleted`),
    KEY `idx_updated_at`   (`updated_at`),
    KEY `idx_deleted_at`   (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2 attribute_group (V002 + V008 adds code column)
CREATE TABLE IF NOT EXISTS `attribute_group` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `product_type_id` BIGINT       NOT NULL,
    `code`            VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `name`            VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `display_order`   INT                   DEFAULT 0,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         BOOLEAN      NOT NULL DEFAULT FALSE,
    `deleted_at`      DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_type_id`) REFERENCES `product_type` (`id`),
    KEY `idx_product_type_id` (`product_type_id`),
    KEY `idx_deleted`         (`deleted`),
    KEY `idx_updated_at`      (`updated_at`),
    KEY `idx_deleted_at`      (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.3 attribute_definition (V002)
CREATE TABLE IF NOT EXISTS `attribute_definition` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `product_type_id`    BIGINT       NOT NULL,
    `attribute_group_id` BIGINT                DEFAULT NULL,
    `code`               VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `name`               VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `data_type`          VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'STRING, TEXT, NUMBER, BOOLEAN, DATE',
    `required`           BOOLEAN      NOT NULL DEFAULT FALSE,
    `searchable`         BOOLEAN      NOT NULL DEFAULT FALSE,
    `filterable`         BOOLEAN      NOT NULL DEFAULT FALSE,
    `display_order`      INT                   DEFAULT 0,
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            BOOLEAN      NOT NULL DEFAULT FALSE,
    `deleted_at`         DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_type_id`)    REFERENCES `product_type`    (`id`),
    FOREIGN KEY (`attribute_group_id`) REFERENCES `attribute_group` (`id`),
    UNIQUE KEY `uk_code_product_type` (`code`, `product_type_id`),
    KEY `idx_product_type_id`    (`product_type_id`),
    KEY `idx_attribute_group_id` (`attribute_group_id`),
    KEY `idx_deleted`            (`deleted`),
    KEY `idx_updated_at`         (`updated_at`),
    KEY `idx_deleted_at`         (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.4 category (V002)
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `parent_id`   BIGINT                DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     BOOLEAN      NOT NULL DEFAULT FALSE,
    `deleted_at`  DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`),
    KEY `idx_parent_id`  (`parent_id`),
    KEY `idx_deleted`    (`deleted`),
    KEY `idx_updated_at` (`updated_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.5 vendors (V023)
CREATE TABLE IF NOT EXISTS `vendors` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(200) NOT NULL COLLATE utf8mb4_unicode_ci,
    `code`          VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `contact_name`  VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `email`         VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `phone`         VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `address`       VARCHAR(300)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `tax_id`        VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `payment_terms` VARCHAR(20)  NOT NULL DEFAULT 'NET_30',
    `is_active`     TINYINT(1)   NOT NULL DEFAULT 1,
    `notes`         VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`    DATETIME              DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.6 product (V002 + V012 cost_price + V033 unit + V035 vendor_id)
CREATE TABLE IF NOT EXISTS `product` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `product_type_id` BIGINT        NOT NULL,
    `sku`             VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `name`            VARCHAR(255)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `description`     VARCHAR(1000)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `price`           DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `cost_price`      DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Standard purchase/cost price',
    `unit`            VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'e.g. cái, kg, gram, lít, hộp, thùng',
    `vendor_id`       BIGINT                 DEFAULT NULL COMMENT 'Primary supplier/vendor',
    `status`          VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         BOOLEAN       NOT NULL DEFAULT FALSE,
    `deleted_at`      DATETIME               DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku`             (`sku`),
    FOREIGN KEY (`product_type_id`) REFERENCES `product_type` (`id`),
    CONSTRAINT `fk_product_vendor`  FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`),
    KEY `idx_product_type_id` (`product_type_id`),
    KEY `idx_status`          (`status`),
    KEY `idx_deleted`         (`deleted`),
    KEY `idx_deleted_at`      (`deleted_at`),
    KEY `idx_created_at`      (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.7 product_category (V002)
CREATE TABLE IF NOT EXISTS `product_category` (
    `product_id`  BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    PRIMARY KEY (`product_id`, `category_id`),
    FOREIGN KEY (`product_id`)  REFERENCES `product`  (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE,
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.8 product_attribute_value (V002 + V009 adds soft-delete)
CREATE TABLE IF NOT EXISTS `product_attribute_value` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`    BIGINT        NOT NULL,
    `attribute_id`  BIGINT        NOT NULL,
    `value_string`  VARCHAR(1000)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `value_number`  DECIMAL(15,4)          DEFAULT NULL,
    `value_boolean` BOOLEAN                DEFAULT NULL,
    `value_date`    DATE                   DEFAULT NULL,
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       BOOLEAN       NOT NULL DEFAULT FALSE,
    `deleted_at`    DATETIME               DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_id`)   REFERENCES `product`              (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`attribute_id`) REFERENCES `attribute_definition` (`id`),
    UNIQUE KEY `uk_product_attribute` (`product_id`, `attribute_id`),
    KEY `idx_product_id`   (`product_id`),
    KEY `idx_attribute_id` (`attribute_id`),
    KEY `idx_deleted`      (`deleted`),
    KEY `idx_deleted_at`   (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.9 variant_types (V026)
CREATE TABLE IF NOT EXISTS `variant_types` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `description`     VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `product_type_id` BIGINT                DEFAULT NULL COMMENT 'NULL = applies to all product types',
    `sort_order`      INT          NOT NULL DEFAULT 0,
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`      DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.10 variant_type_options (V026)
CREATE TABLE IF NOT EXISTS `variant_type_options` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `variant_type_id` BIGINT       NOT NULL,
    `value`           VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `sort_order`      INT          NOT NULL DEFAULT 0,
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`      DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`variant_type_id`) REFERENCES `variant_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 3: Inventory ──────────────────────────────────────

-- 3.1 inventory (V003 + V013 zone/aisle/shelf/bin shelf-location)
CREATE TABLE IF NOT EXISTS `inventory` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`         BIGINT        NOT NULL,
    `quantity_in_stock`  BIGINT        NOT NULL DEFAULT 0,
    `reorder_level`      BIGINT        NOT NULL DEFAULT 10,
    `reorder_quantity`   BIGINT        NOT NULL DEFAULT 50,
    `unit_cost`          DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `warehouse_location` VARCHAR(255)           COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Kho chính',
    `zone`               VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Khu vực kho (e.g. A, MAIN, LANH)',
    `aisle`              VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Hàng (e.g. 1, 2)',
    `shelf`              VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Kệ (e.g. A, B)',
    `bin`                VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ô / Ngăn (e.g. 01, 02)',
    `last_restock_date`  DATETIME               DEFAULT NULL,
    `expiry_date`        DATE                   DEFAULT NULL,
    `batch_number`       VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`              VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `status`             VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, DISCONTINUED',
    `inventory_type`     VARCHAR(50)   NOT NULL DEFAULT 'RETAIL' COMMENT 'RETAIL, WHOLESALE, WAREHOUSE',
    `deleted`            BOOLEAN       NOT NULL DEFAULT FALSE,
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`         DATETIME               DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id`        (`product_id`),
    FOREIGN KEY (`product_id`)        REFERENCES `product` (`id`),
    KEY `idx_product_id`              (`product_id`),
    KEY `idx_warehouse_location`      (`warehouse_location`),
    KEY `idx_status`                  (`status`),
    KEY `idx_inventory_type`          (`inventory_type`),
    KEY `idx_expiry_date`             (`expiry_date`),
    KEY `idx_deleted`                 (`deleted`),
    KEY `idx_created_at`              (`created_at`),
    KEY `idx_low_stock`               (`quantity_in_stock`, `reorder_level`, `deleted`),
    KEY `idx_expired_items`           (`expiry_date`, `deleted`),
    KEY `idx_composite_search`        (`deleted`, `status`, `inventory_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3.2 inventory_movement (V003)
CREATE TABLE IF NOT EXISTS `inventory_movement` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `inventory_id`     BIGINT        NOT NULL,
    `movement_type`    VARCHAR(50)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'IN, OUT, ADJUSTMENT, RETURN, DAMAGE, EXPIRED',
    `quantity`         DECIMAL(15,2) NOT NULL,
    `reference_number` VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `reference_type`   VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_by_user`  VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `reason`           VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`            VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          BOOLEAN       NOT NULL DEFAULT FALSE,
    `deleted_at`       DATETIME               DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_im_inventory` FOREIGN KEY (`inventory_id`) REFERENCES `inventory` (`id`) ON DELETE CASCADE,
    KEY `idx_inventory_id`    (`inventory_id`),
    KEY `idx_movement_type`   (`movement_type`),
    KEY `idx_reference_number` (`reference_number`),
    KEY `idx_created_at`      (`created_at`),
    KEY `idx_deleted`         (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 4: Customers ──────────────────────────────────────

-- 4.1 customers (V001 + V015 id_card + V021 loyalty fields)
CREATE TABLE IF NOT EXISTS `customers` (
    `id`                         BIGINT        NOT NULL AUTO_INCREMENT,
    `name`                       VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `phone`                      VARCHAR(20)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `email`                      VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`                      TEXT                   COLLATE utf8mb4_unicode_ci,
    `zalo_id`                    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `facebook_id`                VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `preferred_services`         VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `allergies_or_sensitivities` VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `hair_type`                  VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `special_requests`           VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `id_card_number`             VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `date_of_birth`              DATE                   DEFAULT NULL,
    `gender`                     VARCHAR(10)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `id_card_issued_date`        DATE                   DEFAULT NULL,
    `id_card_issued_place`       VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `permanent_address`          VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `loyalty_points`             INT           NOT NULL DEFAULT 0,
    `total_spent`                DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `created_at`                 TIMESTAMP     NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                 TIMESTAMP     NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                    TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`                 TIMESTAMP     NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone`          (`phone`),
    UNIQUE KEY `uk_id_card_number` (`id_card_number`),
    KEY `idx_phone`                (`phone`),
    KEY `idx_deleted`              (`deleted`),
    KEY `idx_deleted_at`           (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 5: Orders & Invoices ──────────────────────────────

-- 5.1 invoice_buyers (V019) — must precede invoices (FK target)
CREATE TABLE IF NOT EXISTS `invoice_buyers` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `customer_id`        BIGINT                DEFAULT NULL,
    `buyer_name`         VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_legal_name`   VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_tax_code`     VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_address`      VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_phone_number` VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_email`        VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_bank_name`    VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_bank_account` VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_id_number`    VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `is_visiting_guest`  TINYINT(1)            DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_customer_id`    (`customer_id`),
    KEY `idx_buyer_tax_code` (`buyer_tax_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5.2 orders (V001 + V014 payment + V016 audit + V017 VOIDED + V022 promo/loyalty + V025 table/source)
CREATE TABLE IF NOT EXISTS `orders` (
    `id`                       BIGINT        NOT NULL AUTO_INCREMENT,
    `order_number`             VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `customer_id`              BIGINT                 DEFAULT NULL,
    `status`                   ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED')
                                             COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
    `payment_method`           VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `amount_paid`              DECIMAL(10,2)          DEFAULT NULL,
    `change_amount`            DECIMAL(10,2)          DEFAULT NULL,
    `total_amount`             DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `discount_amount`          DECIMAL(10,2)          DEFAULT 0.00,
    `tax_percentage`           DECIMAL(5,2)           DEFAULT 0.00,
    `tax_amount`               DECIMAL(10,2)          DEFAULT 0.00,
    `commission_amount`        DECIMAL(10,2)          DEFAULT 0.00,
    `invoice_id`               BIGINT                 DEFAULT NULL COMMENT 'Soft ref to invoices; no FK (circular dependency)',
    `notes`                    TEXT                   COLLATE utf8mb4_unicode_ci,
    `created_by`               VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `completed_at`             TIMESTAMP              DEFAULT NULL,
    `completed_by`             VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `cancelled_at`             DATETIME               DEFAULT NULL,
    `cancel_reason`            VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `cancelled_by`             VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `voided_at`                DATETIME               DEFAULT NULL,
    `void_reason`              VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `voided_by`                VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `promotion_code`           VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `promotion_discount`       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `loyalty_points_redeemed`  INT           NOT NULL DEFAULT 0,
    `loyalty_discount`         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `table_label`              VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `source`                   VARCHAR(20)   NOT NULL DEFAULT 'POS',
    `created_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`               TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    CONSTRAINT `fk_orders_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
    KEY `idx_status`      (`status`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_deleted_at`  (`deleted_at`),
    KEY `idx_invoice_id`  (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5.3 order_items (V001 + V012 unit_cost/cost_amount)
CREATE TABLE IF NOT EXISTS `order_items` (
    `id`                    BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`              BIGINT        NOT NULL,
    `product_id`            BIGINT        NOT NULL,
    `product_name`          VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `quantity`              INT           NOT NULL DEFAULT 1,
    `unit_price`            DECIMAL(10,2) NOT NULL,
    `amount`                DECIMAL(10,2) NOT NULL,
    `status`                ENUM('PENDING','IN_PROGRESS','COMPLETED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
    `tax_percentage`        DECIMAL(5,2)           DEFAULT 0.00,
    `tax_amount`            DECIMAL(10,2)          DEFAULT 0.00,
    `commission_rate`       DECIMAL(5,2)           DEFAULT 0.00,
    `commission_amount`     DECIMAL(10,2)          DEFAULT 0.00,
    `amount_before_tax`     DECIMAL(10,2)          DEFAULT 0.00,
    `assigned_employee_id`  BIGINT                 DEFAULT NULL,
    `unit_cost`             DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Cost per unit at time of sale',
    `cost_amount`           DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total cost for this line',
    `included_in_salary_id` BIGINT                 DEFAULT NULL,
    `is_salary_calculated`  TINYINT(1)    NOT NULL DEFAULT 0,
    `completed_at`          DATETIME               DEFAULT NULL,
    `created_at`            TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`            TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_oi_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
    KEY `idx_order_id`          (`order_id`),
    KEY `idx_status`            (`status`),
    KEY `idx_assigned_employee` (`assigned_employee_id`),
    KEY `idx_deleted_at`        (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5.4 invoices (V019 + V038 e-invoice fields)
CREATE TABLE IF NOT EXISTS `invoices` (
    `id`                       BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`                 BIGINT        NOT NULL,
    `invoice_number`           VARCHAR(50)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `invoice_series`           VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `total_amount`             DECIMAL(10,2) NOT NULL,
    `tax`                      DECIMAL(10,2)          DEFAULT 0.00,
    `status`                   ENUM('DRAFT','COMPLETED','FAILED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
    `external_invoice_id`      VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `external_sync_at`         TIMESTAMP              DEFAULT NULL,
    `notes`                    TEXT                   COLLATE utf8mb4_unicode_ci,
    `issued_date`              DATETIME               DEFAULT NULL,
    `total_amount_without_tax` DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `tax_amount`               DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `tax_percentage`           DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    `payment_type`             VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `invoice_type`             VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `currency_code`            VARCHAR(3)             COLLATE utf8mb4_unicode_ci DEFAULT 'VND',
    `error_message`            VARCHAR(1000)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_id`                 BIGINT                 DEFAULT NULL,
    `buyer_name`               VARCHAR(200)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_legal_name`         VARCHAR(200)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_tax_code`           VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_address_line`       VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_phone_number`       VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_email`              VARCHAR(200)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_bank_name`          VARCHAR(200)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_bank_account`       VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `buyer_id_number`          VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `visiting_guest`           TINYINT(1)    NOT NULL DEFAULT 0,
    `customer_id`              BIGINT                 DEFAULT NULL,
    `code_of_tax`              VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_by`               VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `transaction_uuid`         VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`                  TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`               TIMESTAMP              DEFAULT NULL,
    `created_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invoice_number`   (`invoice_number`),
    CONSTRAINT `fk_inv_order`  FOREIGN KEY (`order_id`)  REFERENCES `orders`         (`id`),
    CONSTRAINT `fk_inv_buyer`  FOREIGN KEY (`buyer_id`)  REFERENCES `invoice_buyers` (`id`) ON DELETE SET NULL,
    KEY `idx_order_id`              (`order_id`),
    KEY `idx_status`                (`status`),
    KEY `idx_invoice_number`        (`invoice_number`),
    KEY `idx_deleted_at`            (`deleted_at`),
    KEY `idx_external_invoice_id`   (`external_invoice_id`),
    KEY `idx_deleted`               (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5.5 invoice_items (V019)
CREATE TABLE IF NOT EXISTS `invoice_items` (
    `id`                       BIGINT        NOT NULL AUTO_INCREMENT,
    `invoice_id`               BIGINT        NOT NULL,
    `line_number`              INT                    DEFAULT NULL,
    `order_item_id`            BIGINT                 DEFAULT NULL,
    `service_name`             VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `service_code`             VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `unit`                     VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `unit_price`               DECIMAL(19,2) NOT NULL,
    `quantity`                 DECIMAL(19,2) NOT NULL,
    `discount`                 DECIMAL(19,2)          DEFAULT 0.00,
    `total_amount_without_tax` DECIMAL(19,2) NOT NULL,
    `tax_percentage`           DECIMAL(5,2)           DEFAULT 0.00,
    `tax_amount`               DECIMAL(19,2)          DEFAULT 0.00,
    `total_amount_with_tax`    DECIMAL(19,2) NOT NULL,
    `created_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ii_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_ii_amount` CHECK (`total_amount_with_tax` >= 0),
    KEY `idx_invoice_id`    (`invoice_id`),
    KEY `idx_line_number`   (`line_number`),
    KEY `idx_order_item_id` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 6: POS & Cart ─────────────────────────────────────

-- 6.1 carts (V011)
CREATE TABLE IF NOT EXISTS `carts` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
    `cart_id`            VARCHAR(36)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'UUID for cart session',
    `customer_id`        BIGINT                 DEFAULT NULL,
    `subtotal`           DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `total_discount`     DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `total_tax`          DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `total`              DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `status`             VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ABANDONED, COMPLETED, PAID',
    `applied_coupons`    LONGTEXT               COLLATE utf8mb4_unicode_ci,
    `applied_promotions` LONGTEXT               COLLATE utf8mb4_unicode_ci,
    `notes`              TEXT                   COLLATE utf8mb4_unicode_ci,
    `created_at`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `abandoned_at`       TIMESTAMP              DEFAULT NULL,
    `completed_at`       TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_id` (`cart_id`),
    KEY `idx_cart_id`      (`cart_id`),
    KEY `idx_customer_id`  (`customer_id`),
    KEY `idx_status`       (`status`),
    KEY `idx_created_at`   (`created_at`),
    KEY `idx_updated_at`   (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.2 cart_items (V011 + V012 adds unit_cost)
CREATE TABLE IF NOT EXISTS `cart_items` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `cart_id`         BIGINT        NOT NULL,
    `product_id`      BIGINT        NOT NULL,
    `product_name`    VARCHAR(255)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `sku`             VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `barcode`         VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `quantity`        INT           NOT NULL DEFAULT 1,
    `unit_price`      DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `base_price`      DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `discount_type`   VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'NONE, AMOUNT, PERCENTAGE',
    `discount_value`  DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `discount_reason` VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `line_subtotal`   DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `line_total`      DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `tax`             DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `line_grand_total` DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `unit_cost`       DECIMAL(19,2) NOT NULL DEFAULT 0.00 COMMENT 'Cost per unit at time of cart addition',
    `variants`        JSON                   COMMENT 'Product variants as JSON object',
    `notes`           TEXT                   COLLATE utf8mb4_unicode_ci,
    `added_at`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ci_cart` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`) ON DELETE CASCADE,
    KEY `idx_cart_id`    (`cart_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_added_at`   (`added_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 7: Promotions & Loyalty ──────────────────────────

-- 7.1 promotions (V022)
CREATE TABLE IF NOT EXISTS `promotions` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
    `name`                VARCHAR(200)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `code`                VARCHAR(50)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `type`                VARCHAR(20)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'AMOUNT or PERCENTAGE',
    `value`               DECIMAL(10,2) NOT NULL,
    `min_order_amount`    DECIMAL(10,2)          DEFAULT NULL,
    `max_discount_amount` DECIMAL(10,2)          DEFAULT NULL,
    `start_date`          DATETIME               DEFAULT NULL,
    `end_date`            DATETIME               DEFAULT NULL,
    `usage_limit`         INT                    DEFAULT NULL,
    `used_count`          INT           NOT NULL DEFAULT 0,
    `is_active`           TINYINT(1)    NOT NULL DEFAULT 1,
    `description`         VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`             TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`          DATETIME               DEFAULT NULL,
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME               DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.2 loyalty_programs (V021) — one row per tenant
CREATE TABLE IF NOT EXISTS `loyalty_programs` (
    `id`                             BIGINT        NOT NULL AUTO_INCREMENT,
    `points_per_amount`              INT           NOT NULL DEFAULT 1,
    `amount_per_points`              BIGINT        NOT NULL DEFAULT 10000,
    `redemption_points_per_discount` INT           NOT NULL DEFAULT 100,
    `redemption_discount_amount`     DECIMAL(10,2) NOT NULL DEFAULT 10000,
    `min_redemption_points`          INT           NOT NULL DEFAULT 100,
    `is_active`                      TINYINT(1)    NOT NULL DEFAULT 1,
    `created_at`                     TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                     TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                        TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`                     TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.3 loyalty_tiers (V021)
CREATE TABLE IF NOT EXISTS `loyalty_tiers` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `name`              VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `min_spend`         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `points_multiplier` DECIMAL(5,2)  NOT NULL DEFAULT 1.00,
    `color`             VARCHAR(20)            DEFAULT '#9E9E9E',
    `description`       VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `sort_order`        INT           NOT NULL DEFAULT 0,
    `created_at`        TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`        TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_min_spend` (`min_spend`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.4 loyalty_transactions (V021)
CREATE TABLE IF NOT EXISTS `loyalty_transactions` (
    `id`             BIGINT    NOT NULL AUTO_INCREMENT,
    `customer_id`    BIGINT    NOT NULL,
    `order_id`       BIGINT             DEFAULT NULL,
    `type`           ENUM('EARNED','REDEEMED','ADJUSTED','EXPIRED') NOT NULL,
    `points`         INT       NOT NULL,
    `balance_before` INT       NOT NULL DEFAULT 0,
    `balance_after`  INT       NOT NULL DEFAULT 0,
    `description`    VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1) NOT NULL DEFAULT 0,
    `deleted_at`     TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_lt_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
    CONSTRAINT `fk_lt_order`    FOREIGN KEY (`order_id`)    REFERENCES `orders`    (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_order_id`    (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 8: Employees ──────────────────────────────────────

-- 8.1 employees (V020)
CREATE TABLE IF NOT EXISTS `employees` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `full_name`       VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `phone`           VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `email`           VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `position`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `department`      VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `hire_date`       DATE                  DEFAULT NULL,
    `active`          TINYINT(1)   NOT NULL DEFAULT 1,
    `base_wage`       DECIMAL(15,2)         DEFAULT NULL COMMENT 'Monthly base wage',
    `commission_rate` DECIMAL(5,2)          DEFAULT NULL COMMENT 'Commission % on sales',
    `notes`           TEXT                  COLLATE utf8mb4_unicode_ci,
    `avatar`          VARCHAR(512)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_id`         BIGINT                DEFAULT NULL,
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)           DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`      DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_employee_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 9: Supply Chain ───────────────────────────────────

-- 9.1 purchase_orders (V023)
CREATE TABLE IF NOT EXISTS `purchase_orders` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `po_number`     VARCHAR(30)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `vendor_id`     BIGINT       NOT NULL,
    `status`        VARCHAR(30)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, ORDERED, PARTIAL, RECEIVED, CANCELLED',
    `total_amount`  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `expected_date` DATE                  DEFAULT NULL,
    `ordered_at`    DATETIME              DEFAULT NULL,
    `received_at`   DATETIME              DEFAULT NULL,
    `created_by`    VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`         VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`    DATETIME              DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_po_number` (`po_number`),
    CONSTRAINT `fk_po_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9.2 purchase_order_items (V023)
CREATE TABLE IF NOT EXISTS `purchase_order_items` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
    `purchase_order_id`  BIGINT        NOT NULL,
    `product_id`         BIGINT                 DEFAULT NULL,
    `product_name`       VARCHAR(255)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `product_sku`        VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `quantity_ordered`   INT           NOT NULL,
    `quantity_received`  INT           NOT NULL DEFAULT 0,
    `unit_cost`          DECIMAL(15,2) NOT NULL,
    `total_cost`         DECIMAL(15,2) NOT NULL,
    `deleted`            TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`         DATETIME               DEFAULT NULL,
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME               DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_poi_po` FOREIGN KEY (`purchase_order_id`) REFERENCES `purchase_orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 10: Buyback / Commodity Market ────────────────────

-- 10.1 market_prices (V030) — commodity valuation for buyback/gold shops
CREATE TABLE IF NOT EXISTS `market_prices` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `unit`        VARCHAR(20)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `buy_price`   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `sell_price`  DECIMAL(15,2)          DEFAULT NULL,
    `is_active`   TINYINT(1)    NOT NULL DEFAULT 1,
    `notes`       VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `sort_order`  INT           NOT NULL DEFAULT 999,
    `created_at`  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`  TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_active`     (`is_active`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_deleted`    (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10.2 buyback_orders (V030)
CREATE TABLE IF NOT EXISTS `buyback_orders` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `order_number`   VARCHAR(30)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `type`           VARCHAR(20)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'BUY | EXCHANGE',
    `status`         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPLETED | CANCELLED',
    `customer_id`    BIGINT                 DEFAULT NULL,
    `customer_name`  VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `customer_phone` VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `payment_method` VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    `buy_total`      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `sale_total`     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `net_amount`     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `notes`          VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_by`     VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `completed_at`   TIMESTAMP              DEFAULT NULL,
    `completed_by`   VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `cancelled_at`   TIMESTAMP              DEFAULT NULL,
    `cancelled_by`   VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`     TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`     TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bb_order_number` (`order_number`),
    KEY `idx_type`        (`type`),
    KEY `idx_status`      (`status`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_created_at`  (`created_at`),
    KEY `idx_deleted`     (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10.3 buyback_order_items (V030)
CREATE TABLE IF NOT EXISTS `buyback_order_items` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `buyback_order_id` BIGINT        NOT NULL,
    `item_type`        VARCHAR(10)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'BUY | SALE',
    `commodity_id`     BIGINT                 DEFAULT NULL,
    `commodity_name`   VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `unit`             VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `weight`           DECIMAL(10,3)          DEFAULT NULL,
    `condition_type`   VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'NEW | USED | SCRAP',
    `price_per_unit`   DECIMAL(15,2)          DEFAULT NULL,
    `product_name`     VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `quantity`         INT                    DEFAULT NULL,
    `unit_price`       DECIMAL(15,2)          DEFAULT NULL,
    `total_price`      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `notes`            VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`       TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`       TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_boi_order` FOREIGN KEY (`buyback_order_id`) REFERENCES `buyback_orders` (`id`) ON DELETE CASCADE,
    KEY `idx_order_id`  (`buyback_order_id`),
    KEY `idx_item_type` (`item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 11: Pawn (V037) ───────────────────────────────────

-- 11.1 pawn
CREATE TABLE IF NOT EXISTS `pawn` (
    `pawn_id`                BIGINT         NOT NULL AUTO_INCREMENT,
    `customer_id`            BIGINT                  DEFAULT NULL,
    `item_name`              VARCHAR(255)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `item_description`       TEXT                    COLLATE utf8mb4_unicode_ci,
    `item_weight`            DECIMAL(10,3)           DEFAULT NULL,
    `gem_weight`             DECIMAL(10,3)           DEFAULT NULL,
    `item_value`             DECIMAL(15,2)           DEFAULT NULL,
    `item_type`              VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `item_brand`             VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `pawn_date`              DATETIME                DEFAULT NULL,
    `pawn_due_date`          DATETIME                DEFAULT NULL,
    `pawn_amount`            DECIMAL(15,2)           DEFAULT NULL,
    `interest_rate`          DECIMAL(10,4)           DEFAULT NULL,
    `status`                 VARCHAR(50)             COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_by`             VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`             DATETIME                DEFAULT NULL,
    `updated_by`             VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at`             DATETIME                DEFAULT NULL,
    `canceled_reason`        TEXT                    COLLATE utf8mb4_unicode_ci,
    `total_amount`           DECIMAL(15,2)           DEFAULT NULL,
    `redeem_date`            DATETIME                DEFAULT NULL,
    `interest_amount`        DECIMAL(15,2)           DEFAULT NULL,
    `forfeited_reason`       TEXT                    COLLATE utf8mb4_unicode_ci,
    `forfeited_amount`       DECIMAL(15,2)           DEFAULT NULL,
    `forfeited_date`         DATETIME                DEFAULT NULL,
    `original_id`            BIGINT                  DEFAULT NULL,
    `interest_days_per_month` INT                    DEFAULT NULL,
    `pawned_days`            INT                     DEFAULT NULL,
    `visible`                BOOLEAN                 NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`pawn_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11.2 pawn_audit
CREATE TABLE IF NOT EXISTS `pawn_audit` (
    `action_id`              BIGINT         NOT NULL AUTO_INCREMENT,
    `action_type`            VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `action_time`            DATETIME                DEFAULT NULL,
    `pawn_id`                BIGINT                  DEFAULT NULL,
    `customer_id`            BIGINT                  DEFAULT NULL,
    `item_name`              VARCHAR(255)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `item_description`       TEXT                    COLLATE utf8mb4_unicode_ci,
    `item_weight`            DECIMAL(10,3)           DEFAULT NULL,
    `gem_weight`             DECIMAL(10,3)           DEFAULT NULL,
    `item_value`             DECIMAL(15,2)           DEFAULT NULL,
    `item_type`              VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `item_brand`             VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `pawn_date`              DATETIME                DEFAULT NULL,
    `pawn_due_date`          DATETIME                DEFAULT NULL,
    `pawn_amount`            DECIMAL(15,2)           DEFAULT NULL,
    `interest_rate`          DECIMAL(10,4)           DEFAULT NULL,
    `status`                 VARCHAR(50)             COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `canceled_reason`        TEXT                    COLLATE utf8mb4_unicode_ci,
    `total_amount`           DECIMAL(15,2)           DEFAULT NULL,
    `redeem_date`            DATETIME                DEFAULT NULL,
    `interest_amount`        DECIMAL(15,2)           DEFAULT NULL,
    `forfeited_reason`       TEXT                    COLLATE utf8mb4_unicode_ci,
    `forfeited_amount`       DECIMAL(15,2)           DEFAULT NULL,
    `forfeited_date`         DATETIME                DEFAULT NULL,
    `original_id`            BIGINT                  DEFAULT NULL,
    `interest_days_per_month` INT                    DEFAULT NULL,
    `created_by`             VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`             DATETIME                DEFAULT NULL,
    `updated_by`             VARCHAR(100)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at`             DATETIME                DEFAULT NULL,
    PRIMARY KEY (`action_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11.3 pawn_req_money
CREATE TABLE IF NOT EXISTS `pawn_req_money` (
    `request_id`    BIGINT        NOT NULL AUTO_INCREMENT,
    `pawn_id`       BIGINT                 DEFAULT NULL,
    `request_amount` DECIMAL(15,2)         DEFAULT NULL,
    `request_date`  DATETIME               DEFAULT NULL,
    `created_by`    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`    DATETIME               DEFAULT NULL,
    `updated_by`    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at`    DATETIME               DEFAULT NULL,
    PRIMARY KEY (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11.4 pawn_req_money_audit
CREATE TABLE IF NOT EXISTS `pawn_req_money_audit` (
    `action_id`     BIGINT        NOT NULL AUTO_INCREMENT,
    `action_type`   VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `action_time`   DATETIME               DEFAULT NULL,
    `request_id`    BIGINT                 DEFAULT NULL,
    `pawn_id`       BIGINT                 DEFAULT NULL,
    `request_amount` DECIMAL(15,2)         DEFAULT NULL,
    `request_date`  DATETIME               DEFAULT NULL,
    `created_by`    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`    DATETIME               DEFAULT NULL,
    `updated_by`    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at`    DATETIME               DEFAULT NULL,
    PRIMARY KEY (`action_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 12: Shop Configuration ───────────────────────────

-- 12.1 shop_info — identity data only (V043 removed config columns; see shop_config table)
CREATE TABLE IF NOT EXISTS `shop_info` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `shop_name`          VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci DEFAULT 'Cửa hàng của tôi',
    `address`            VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `company_name`       VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `phone`              VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT '',
    `email`              VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `supplier_tax_code`  VARCHAR(150)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `website`            VARCHAR(200)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `created_at`         TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`         TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_deleted`    (`deleted`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12.2 shop_config (V040) — key-value configuration store
CREATE TABLE IF NOT EXISTS `shop_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `config_key`   VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `config_value` TEXT                  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `config_group` VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `encrypted`    BIT(1)       NOT NULL DEFAULT 0,
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      BIT(1)       NOT NULL DEFAULT 0,
    `deleted_at`   TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_shop_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12.3 print_templates (V028 named templates + is_default)
CREATE TABLE IF NOT EXISTS `print_templates` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `template_type` VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `name`          VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci DEFAULT 'Mặc định',
    `config_json`   TEXT         NOT NULL COLLATE utf8mb4_unicode_ci,
    `is_default`    TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`    TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_print_templates_type_name` (`template_type`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12.4 bank_accounts (V031) — shop's own bank accounts for QR payments
CREATE TABLE IF NOT EXISTS `bank_accounts` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `bank_bin`        VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `bank_code`       VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `bank_name`       VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `bank_short_name` VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `account_number`  VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `account_name`    VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `is_default`      TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`      DATETIME(6)           DEFAULT NULL,
    `updated_at`      DATETIME(6)           DEFAULT NULL,
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`      DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_deleted`    (`deleted`),
    KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12.5 shop_expense (V042)
CREATE TABLE IF NOT EXISTS `shop_expense` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `amount`           DECIMAL(20,0) NOT NULL,
    `category`         VARCHAR(30)   NOT NULL,
    `description`      VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `expense_date`     DATE          NOT NULL,
    `payment_method`   VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `reference_number` VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_by`       VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_by`       VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`       DATETIME(6)            DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`       DATETIME(6)            DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_expense_date`     (`expense_date`),
    INDEX `idx_expense_category` (`category`),
    INDEX `idx_expense_deleted`  (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 13: Notifications & Activity ──────────────────────

-- 13.1 notifications (V027)
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Target username',
    `title`          VARCHAR(200) NOT NULL COLLATE utf8mb4_unicode_ci,
    `message`        TEXT                  COLLATE utf8mb4_unicode_ci,
    `type`           VARCHAR(30)  NOT NULL DEFAULT 'INFO' COMMENT 'SYSTEM, ORDER, ANNOUNCEMENT, LOW_STOCK, INFO',
    `reference_type` VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `reference_id`   BIGINT                DEFAULT NULL,
    `is_read`        TINYINT(1)   NOT NULL DEFAULT 0,
    `read_at`        DATETIME(6)           DEFAULT NULL,
    `created_by`     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    `created_at`     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`     DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notifications_user_read` (`user_id`, `is_read`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13.2 activity_log (V036)
CREATE TABLE IF NOT EXISTS `activity_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `actor_username`  VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Username who performed the action',
    `actor_full_name` VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `action`          VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'ActivityAction enum value',
    `target_type`     VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ORDER, PRODUCT, CUSTOMER, etc.',
    `target_id`       VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `description`     VARCHAR(500) NOT NULL COLLATE utf8mb4_unicode_ci,
    `ip_address`      VARCHAR(45)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_activity_actor`      (`actor_username`),
    INDEX `idx_activity_action`     (`action`),
    INDEX `idx_activity_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13.3 api_audit_log
CREATE TABLE IF NOT EXISTS `api_audit_log` (
    `log_id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `trace_id`              VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `api_endpoint`          VARCHAR(500) NOT NULL COLLATE utf8mb4_unicode_ci,
    `http_method`           VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `request_body`          LONGTEXT              COLLATE utf8mb4_unicode_ci,
    `request_headers`       LONGTEXT              COLLATE utf8mb4_unicode_ci,
    `response_body`         LONGTEXT              COLLATE utf8mb4_unicode_ci,
    `response_headers`      LONGTEXT              COLLATE utf8mb4_unicode_ci,
    `response_status`       INT                   DEFAULT NULL,
    `request_size`          BIGINT                DEFAULT NULL,
    `response_size`         BIGINT                DEFAULT NULL,
    `execution_time_ms`     BIGINT                DEFAULT NULL,
    `error_message`         LONGTEXT              COLLATE utf8mb4_unicode_ci,
    `exception_stack_trace` LONGTEXT              COLLATE utf8mb4_unicode_ci,
    `user_id`               VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `ip_address`            VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `status`                VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SUCCESS, FAILURE',
    `description`           VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`               TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`            TIMESTAMP    NULL DEFAULT NULL,
    `created_at`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    KEY `idx_api_endpoint` (`api_endpoint`(255)),
    KEY `idx_method`       (`http_method`),
    KEY `idx_timestamp`    (`created_at`),
    KEY `idx_status`       (`response_status`),
    KEY `idx_trace_id`     (`trace_id`),
    KEY `idx_deleted`      (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 14: Gold Price Board (V039) ──────────────────────

-- 14.1 gold_price — customer-facing TV display (pawn / gold shops)
CREATE TABLE IF NOT EXISTS `gold_price` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `code`          VARCHAR(50)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `label`         VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `buy`           DECIMAL(20,0) NOT NULL DEFAULT 0,
    `sell`          DECIMAL(20,0) NOT NULL DEFAULT 0,
    `pawn`          DECIMAL(20,0) NOT NULL DEFAULT 0,
    `display_order` INT           NOT NULL DEFAULT 10,
    `note`          VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `show_in_board` BIT(1)        NOT NULL DEFAULT 1,
    `created_by`    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_by`    VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       BIT(1)        NOT NULL DEFAULT 0,
    `deleted_at`    TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── SECTION 15: Banks reference (same as master) ──────────────

-- 15.1 banks — VietQR reference table for QR payment generation
CREATE TABLE IF NOT EXISTS `banks` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `bin`         VARCHAR(10)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `name`        VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `short_name`  VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `sort_order`  INT          NOT NULL DEFAULT 999,
    `is_active`   TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`  TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`  TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code`    (`code`),
    KEY `idx_code`          (`code`),
    KEY `idx_sort_order`    (`sort_order`),
    KEY `idx_active`        (`is_active`),
    KEY `idx_deleted`       (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
