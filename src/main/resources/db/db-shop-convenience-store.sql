-- ============================================================
-- TENANT DATABASE - CONVENIENCE STORE (TẠP HÓA) SETUP
-- Database: retail-platform-taphoamau  (rename as needed)
-- All Flyway migrations V001-V035 are consolidated here.
-- Includes: banks (VietQR reference), full product catalog, inventory.
-- Run this against a fresh tenant database to get a fully
-- operational convenience store with sample products.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Replace `retail-platform-taphoamau` with the actual tenant db name
CREATE DATABASE IF NOT EXISTS `retail-platform-taphoamau`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `retail-platform-taphoamau`;

-- ──────────────────────────────────────────────────────────────
-- SECTION 1: TABLE DEFINITIONS  (final state, all migrations)
-- ──────────────────────────────────────────────────────────────

-- 1.1 api_audit_log
CREATE TABLE IF NOT EXISTS `api_audit_log` (
    `log_id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `trace_id`              VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Unique trace ID',
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
    `status`                VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SUCCESS, FAILURE, PARTIAL_FAILURE',
    `description`           VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`               TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`            TIMESTAMP    NULL     DEFAULT NULL,
    `created_at`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    KEY `idx_api_endpoint` (`api_endpoint`(255)),
    KEY `idx_method`       (`http_method`),
    KEY `idx_timestamp`    (`created_at`),
    KEY `idx_status`       (`response_status`),
    KEY `idx_trace_id`     (`trace_id`),
    KEY `idx_deleted`      (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.2 features
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
    UNIQUE KEY `uk_name`  (`name`),
    KEY `idx_name`        (`name`),
    KEY `idx_active`      (`active`),
    KEY `idx_deleted`     (`deleted`),
    KEY `idx_deleted_at`  (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.3 roles
CREATE TABLE IF NOT EXISTS `roles` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `description` VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`  TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`  TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name`  (`name`),
    KEY `idx_name`        (`name`),
    KEY `idx_deleted`     (`deleted`),
    KEY `idx_deleted_at`  (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.4 users
-- Includes failed_login_attempts added by V032
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
    UNIQUE KEY `uk_username`  (`username`),
    KEY `idx_username`        (`username`),
    KEY `idx_active`          (`active`),
    KEY `idx_users_active`    (`active`, `username`)
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
    CONSTRAINT `fk_rt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
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
  COMMENT='Maps roles to features for role-based access control';

-- 1.7 user_roles
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.8 active_sessions (V034)
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

-- 1.9 product_type (V002)
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
    UNIQUE KEY `uk_code`  (`code`),
    KEY `idx_code`        (`code`),
    KEY `idx_deleted`     (`deleted`),
    KEY `idx_updated_at`  (`updated_at`),
    KEY `idx_deleted_at`  (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.10 attribute_group (V002 + V008 adds code column)
CREATE TABLE IF NOT EXISTS `attribute_group` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `product_type_id` BIGINT       NOT NULL,
    `code`            VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Added by V008',
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

-- 1.11 attribute_definition (V002)
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

-- 1.12 category (V002)
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

-- 1.13 vendors (V023)
CREATE TABLE IF NOT EXISTS `vendors` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(200) NOT NULL COLLATE utf8mb4_unicode_ci,
    `code`          VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci UNIQUE,
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
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.14 product
-- Final state after V002 (base) + V012 (cost_price) + V033 (unit) + V035 (vendor_id FK)
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
    UNIQUE KEY `uk_sku`       (`sku`),
    FOREIGN KEY (`product_type_id`) REFERENCES `product_type` (`id`),
    CONSTRAINT `fk_product_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`),
    KEY `idx_product_type_id` (`product_type_id`),
    KEY `idx_status`          (`status`),
    KEY `idx_deleted`         (`deleted`),
    KEY `idx_deleted_at`      (`deleted_at`),
    KEY `idx_created_at`      (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.15 product_category (V002)
CREATE TABLE IF NOT EXISTS `product_category` (
    `product_id`  BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    PRIMARY KEY (`product_id`, `category_id`),
    FOREIGN KEY (`product_id`)  REFERENCES `product`  (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE,
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.16 product_attribute_value (V002 + V009 adds soft delete)
CREATE TABLE IF NOT EXISTS `product_attribute_value` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `product_id`   BIGINT          NOT NULL,
    `attribute_id` BIGINT          NOT NULL,
    `value_string` VARCHAR(1000)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `value_number` DECIMAL(15,4)            DEFAULT NULL,
    `value_boolean` BOOLEAN                 DEFAULT NULL,
    `value_date`   DATE                     DEFAULT NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      BOOLEAN         NOT NULL DEFAULT FALSE,
    `deleted_at`   DATETIME                 DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_id`)   REFERENCES `product`              (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`attribute_id`) REFERENCES `attribute_definition` (`id`),
    UNIQUE KEY `uk_product_attribute` (`product_id`, `attribute_id`),
    KEY `idx_product_id`   (`product_id`),
    KEY `idx_attribute_id` (`attribute_id`),
    KEY `idx_deleted`      (`deleted`),
    KEY `idx_deleted_at`   (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.17 variant_types (V026)
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

-- 1.18 variant_type_options (V026)
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

-- 1.19 inventory
-- Final state after V003 (base) + V013 (zone, aisle, shelf, bin)
CREATE TABLE IF NOT EXISTS `inventory` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`        BIGINT        NOT NULL,
    `quantity_in_stock` BIGINT        NOT NULL DEFAULT 0,
    `reorder_level`     BIGINT        NOT NULL DEFAULT 10,
    `reorder_quantity`  BIGINT        NOT NULL DEFAULT 50,
    `unit_cost`         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `warehouse_location` VARCHAR(255)          COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Kho chính',
    `zone`              VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Khu vực / Kho (e.g. A, MAIN, LANH)',
    `aisle`             VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Hàng (e.g. 1, 2, 3)',
    `shelf`             VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Kệ (e.g. A, B, C)',
    `bin`               VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ô / Ngăn (e.g. 01, 02)',
    `last_restock_date` DATETIME               DEFAULT NULL,
    `expiry_date`       DATE                   DEFAULT NULL,
    `batch_number`      VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`             VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `status`            VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, DISCONTINUED',
    `inventory_type`    VARCHAR(50)   NOT NULL DEFAULT 'RETAIL' COMMENT 'RETAIL, WHOLESALE, WAREHOUSE',
    `deleted`           BOOLEAN       NOT NULL DEFAULT FALSE,
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        DATETIME               DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`),
    FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
    KEY `idx_product_id`        (`product_id`),
    KEY `idx_warehouse_location` (`warehouse_location`),
    KEY `idx_status`            (`status`),
    KEY `idx_inventory_type`    (`inventory_type`),
    KEY `idx_expiry_date`       (`expiry_date`),
    KEY `idx_deleted`           (`deleted`),
    KEY `idx_created_at`        (`created_at`),
    KEY `idx_low_stock`         (`quantity_in_stock`, `reorder_level`, `deleted`),
    KEY `idx_expired_items`     (`expiry_date`, `deleted`),
    KEY `idx_composite_search`  (`deleted`, `status`, `inventory_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.20 inventory_movement (V003)
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

-- 1.21 customers
-- Final state after V001 (base) + V015 (id card fields) + V021 (loyalty fields)
CREATE TABLE IF NOT EXISTS `customers` (
    `id`                        BIGINT        NOT NULL AUTO_INCREMENT,
    `name`                      VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `phone`                     VARCHAR(20)   NOT NULL COLLATE utf8mb4_unicode_ci,
    `email`                     VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`                     TEXT                   COLLATE utf8mb4_unicode_ci,
    `zalo_id`                   VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `facebook_id`               VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `preferred_services`        VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `allergies_or_sensitivities` VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `hair_type`                 VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `special_requests`          VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    -- V015: identity card fields
    `id_card_number`            VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `date_of_birth`             DATE                   DEFAULT NULL,
    `gender`                    VARCHAR(10)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `id_card_issued_date`       DATE                   DEFAULT NULL,
    `id_card_issued_place`      VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `permanent_address`         VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    -- V021: loyalty fields
    `loyalty_points`            INT           NOT NULL DEFAULT 0,
    `total_spent`               DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `created_at`                TIMESTAMP     NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                TIMESTAMP     NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                   TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`                TIMESTAMP     NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone`         (`phone`),
    UNIQUE KEY `uk_id_card_number` (`id_card_number`),
    KEY `idx_phone`               (`phone`),
    KEY `idx_deleted`             (`deleted`),
    KEY `idx_deleted_at`          (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.22 invoice_buyers (V019)
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

-- 1.23 orders
-- Final state after V001 (base) + V014 (payment fields) + V016 (audit fields)
--   + V017 (VOIDED status) + V022 (promo/loyalty fields) + V025 (table_label, source)
CREATE TABLE IF NOT EXISTS `orders` (
    `id`                        BIGINT        NOT NULL AUTO_INCREMENT,
    `order_number`              VARCHAR(20)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `customer_id`               BIGINT                 DEFAULT NULL,
    `status`                    ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED')
                                              COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
    -- V014
    `payment_method`            VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `amount_paid`               DECIMAL(10,2)          DEFAULT NULL,
    `change_amount`             DECIMAL(10,2)          DEFAULT NULL,
    `total_amount`              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `discount_amount`           DECIMAL(10,2)          DEFAULT 0.00,
    `tax_percentage`            DECIMAL(5,2)           DEFAULT 0.00,
    `tax_amount`                DECIMAL(10,2)          DEFAULT 0.00,
    `commission_amount`         DECIMAL(10,2)          DEFAULT 0.00,
    `invoice_id`                BIGINT                 DEFAULT NULL COMMENT 'Soft reference to invoices; no FK to avoid circular dependency',
    `notes`                     TEXT                   COLLATE utf8mb4_unicode_ci,
    -- V016
    `created_by`                VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `completed_at`              TIMESTAMP              DEFAULT NULL,
    `completed_by`              VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `cancelled_at`              DATETIME               DEFAULT NULL,
    `cancel_reason`             VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `cancelled_by`              VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `voided_at`                 DATETIME               DEFAULT NULL,
    `void_reason`               VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `voided_by`                 VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    -- V022
    `promotion_code`            VARCHAR(50)            COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `promotion_discount`        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `loyalty_points_redeemed`   INT           NOT NULL DEFAULT 0,
    `loyalty_discount`          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    -- V025
    `table_label`               VARCHAR(100)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `source`                    VARCHAR(20)   NOT NULL DEFAULT 'POS',
    `created_at`                TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                   TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`                TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    CONSTRAINT `fk_orders_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
    KEY `idx_status`       (`status`),
    KEY `idx_customer_id`  (`customer_id`),
    KEY `idx_deleted_at`   (`deleted_at`),
    KEY `idx_invoice_id`   (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.24 employees (V020)
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
    `commission_rate` DECIMAL(5,2)          DEFAULT NULL COMMENT 'Commission percentage on sales',
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

-- 1.25 order_items
-- Final state after V001 (base) + V012 (unit_cost, cost_amount)
CREATE TABLE IF NOT EXISTS `order_items` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`             BIGINT        NOT NULL,
    `product_id`           BIGINT        NOT NULL,
    `product_name`         VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `quantity`             INT           NOT NULL DEFAULT 1,
    `unit_price`           DECIMAL(10,2) NOT NULL,
    `amount`               DECIMAL(10,2) NOT NULL,
    `status`               ENUM('PENDING','IN_PROGRESS','COMPLETED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
    `tax_percentage`       DECIMAL(5,2)           DEFAULT 0.00,
    `tax_amount`           DECIMAL(10,2)          DEFAULT 0.00,
    `commission_rate`      DECIMAL(5,2)           DEFAULT 0.00,
    `commission_amount`    DECIMAL(10,2)          DEFAULT 0.00,
    `amount_before_tax`    DECIMAL(10,2)          DEFAULT 0.00,
    `assigned_employee_id` BIGINT                 DEFAULT NULL,
    -- V012
    `unit_cost`            DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Cost per unit at time of sale',
    `cost_amount`          DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total cost for this line',
    `included_in_salary_id` BIGINT               DEFAULT NULL,
    `is_salary_calculated` TINYINT(1)    NOT NULL DEFAULT 0,
    `completed_at`         DATETIME               DEFAULT NULL,
    `created_at`           TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`              TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`           TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_oi_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
    KEY `idx_order_id`          (`order_id`),
    KEY `idx_status`            (`status`),
    KEY `idx_assigned_employee` (`assigned_employee_id`),
    KEY `idx_deleted_at`        (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.26 invoices (V019)
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
    `transaction_uuid`         VARCHAR(255)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted`                  TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`               TIMESTAMP              DEFAULT NULL,
    `created_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               TIMESTAMP              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invoice_number` (`invoice_number`),
    CONSTRAINT `fk_inv_order`  FOREIGN KEY (`order_id`)  REFERENCES `orders`         (`id`),
    CONSTRAINT `fk_inv_buyer`  FOREIGN KEY (`buyer_id`)  REFERENCES `invoice_buyers` (`id`) ON DELETE SET NULL,
    KEY `idx_order_id`            (`order_id`),
    KEY `idx_status`              (`status`),
    KEY `idx_invoice_number`      (`invoice_number`),
    KEY `idx_deleted_at`          (`deleted_at`),
    KEY `idx_external_invoice_id` (`external_invoice_id`),
    KEY `idx_deleted`             (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.27 invoice_items (V019)
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

-- 1.28 carts (V011)
CREATE TABLE IF NOT EXISTS `carts` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
    `cart_id`            VARCHAR(36)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'UUID for cart session',
    `customer_id`        BIGINT                 DEFAULT NULL,
    `subtotal`           DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `total_discount`     DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `total_tax`          DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `total`              DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    `status`             VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ABANDONED, COMPLETED, PAID',
    `applied_coupons`    LONGTEXT               COLLATE utf8mb4_unicode_ci COMMENT 'JSON array of coupon codes',
    `applied_promotions` LONGTEXT               COLLATE utf8mb4_unicode_ci COMMENT 'JSON array of promotion IDs',
    `notes`              TEXT                   COLLATE utf8mb4_unicode_ci,
    `created_at`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `abandoned_at`       TIMESTAMP              DEFAULT NULL,
    `completed_at`       TIMESTAMP              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_id` (`cart_id`),
    KEY `idx_cart_id`       (`cart_id`),
    KEY `idx_customer_id`   (`customer_id`),
    KEY `idx_status`        (`status`),
    KEY `idx_created_at`    (`created_at`),
    KEY `idx_updated_at`    (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Shopping carts for POS system';

-- 1.29 cart_items (V011 + V012 adds unit_cost)
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
    -- V012
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

-- 1.30 loyalty_programs (V021)
CREATE TABLE IF NOT EXISTS `loyalty_programs` (
    `id`                              BIGINT        NOT NULL AUTO_INCREMENT,
    `points_per_amount`               INT           NOT NULL DEFAULT 1 COMMENT 'Points earned per amount_per_points VND spent',
    `amount_per_points`               BIGINT        NOT NULL DEFAULT 10000 COMMENT 'VND required to earn points_per_amount points',
    `redemption_points_per_discount`  INT           NOT NULL DEFAULT 100 COMMENT 'Points needed to get redemption_discount_amount VND off',
    `redemption_discount_amount`      DECIMAL(10,2) NOT NULL DEFAULT 10000 COMMENT 'VND discount per redemption unit',
    `min_redemption_points`           INT           NOT NULL DEFAULT 100 COMMENT 'Minimum points balance required to redeem',
    `is_active`                       TINYINT(1)    NOT NULL DEFAULT 1,
    `created_at`                      TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                      TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                         TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`                      TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.31 loyalty_tiers (V021)
CREATE TABLE IF NOT EXISTS `loyalty_tiers` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `name`              VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `min_spend`         DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Minimum lifetime spend (VND) to reach this tier',
    `points_multiplier` DECIMAL(5,2)  NOT NULL DEFAULT 1.00 COMMENT 'Point earning multiplier at this tier',
    `color`             VARCHAR(20)            DEFAULT '#9E9E9E' COMMENT 'Hex color for UI display',
    `description`       VARCHAR(500)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `sort_order`        INT           NOT NULL DEFAULT 0,
    `created_at`        TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`        TIMESTAMP     NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_min_spend` (`min_spend`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.32 loyalty_transactions (V021)
CREATE TABLE IF NOT EXISTS `loyalty_transactions` (
    `id`             BIGINT    NOT NULL AUTO_INCREMENT,
    `customer_id`    BIGINT    NOT NULL,
    `order_id`       BIGINT             DEFAULT NULL,
    `type`           ENUM('EARNED','REDEEMED','ADJUSTED','EXPIRED') NOT NULL,
    `points`         INT       NOT NULL COMMENT 'Positive = earned; negative = redeemed/expired',
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

-- 1.33 promotions (V022)
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

-- 1.34 purchase_orders (V023)
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

-- 1.35 purchase_order_items (V023)
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

-- 1.36 market_prices (V030 — buyback valuation for gold/jewelry shops; included for completeness)
CREATE TABLE IF NOT EXISTS `market_prices` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Commodity name',
    `unit`        VARCHAR(20)   NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Weight/measure unit',
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

-- 1.37 buyback_orders (V030)
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

-- 1.38 buyback_order_items (V030)
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

-- 1.39 banks (VietQR reference table)
CREATE TABLE IF NOT EXISTS `banks` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Short bank code (e.g. VCB, TCB)',
    `bin`         VARCHAR(10)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'VietQR BIN code for QR generation',
    `name`        VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Full official bank name',
    `short_name`  VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Common short name',
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

-- 1.40 bank_accounts (V031)
CREATE TABLE IF NOT EXISTS `bank_accounts` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `bank_bin`        VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'VietQR BIN code',
    `bank_code`       VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `bank_name`       VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `bank_short_name` VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `account_number`  VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `account_name`    VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Account holder name (uppercase)',
    `is_default`      TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`      DATETIME(6)           DEFAULT NULL,
    `updated_at`      DATETIME(6)           DEFAULT NULL,
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`      DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_deleted`     (`deleted`),
    KEY `idx_is_default`  (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.40 notifications (V027)
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Target username',
    `title`          VARCHAR(200) NOT NULL COLLATE utf8mb4_unicode_ci,
    `message`        TEXT                  COLLATE utf8mb4_unicode_ci,
    `type`           VARCHAR(30)  NOT NULL DEFAULT 'INFO' COMMENT 'SYSTEM, ORDER, ANNOUNCEMENT, LOW_STOCK, INFO',
    `reference_type` VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ORDER, PRODUCT, INVENTORY',
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

-- 1.41 shop_info
-- Final state after V001 (base) + V018 (cash_denominations) + V024 (pos_mode)
--   + V029 (e_invoice_password/key → TEXT)
CREATE TABLE IF NOT EXISTS `shop_info` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `shop_name`          VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci DEFAULT 'Cửa hàng của tôi',
    `address`            VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `company_name`       VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `default_tax_rate`   DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    `e_invoice_username` VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `e_invoice_password` TEXT                  COLLATE utf8mb4_unicode_ci COMMENT 'AES-256 encrypted',
    `e_invoice_key`      TEXT                  COLLATE utf8mb4_unicode_ci COMMENT 'AES-256 encrypted',
    `phone`              VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT '',
    `email`              VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `supplier_tax_code`  VARCHAR(150)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `invoice_vendor`     VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT '',
    `website`            VARCHAR(200)          COLLATE utf8mb4_unicode_ci DEFAULT '',
    `template_code`      VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `invoice_system`     VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT 'MOCK' COMMENT 'S-INVOICE, M-INVOICE, MOCK',
    `invoice_series`     VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `cash_denominations` VARCHAR(500)          COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Comma-separated denomination values for POS cash pad',
    `pos_mode`           VARCHAR(20)  NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD (numbered tabs) or TABLE (restaurant mode)',
    `created_at`         TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`         TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_deleted`    (`deleted`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.42 print_templates
-- Final state after V001 (base) + V028 (name, is_default columns, composite unique)
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


-- ──────────────────────────────────────────────────────────────
-- SECTION 2: DEFAULT DATA — CONVENIENCE STORE (TẠP HÓA)
-- ──────────────────────────────────────────────────────────────

-- 2.1 Features
INSERT IGNORE INTO `features` (`id`, `name`, `display_name`, `description`, `active`, `created_at`, `updated_at`, `deleted`, `deleted_at`) VALUES
(202601001, 'DASHBOARD',  'Bảng Điều Khiển',     'Xem tổng quan và thống kê chính của cửa hàng',                1, NOW(), NOW(), 0, NULL),
(202601002, 'ORDER',      'Đơn Hàng',             'Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',  1, NOW(), NOW(), 0, NULL),
(202601003, 'MY_WORK',    'Công Việc Của Tôi',    'Xem công việc được giao cho nhân viên hiện tại',              1, NOW(), NOW(), 0, NULL),
(202601004, 'PRODUCT',    'Sản Phẩm & Hàng Hóa', 'Quản lý danh sách hàng hóa, giá cả và thông tin sản phẩm',   1, NOW(), NOW(), 0, NULL),
(202601005, 'PROMOTION',  'Khuyến Mãi',           'Tạo và quản lý các chương trình khuyến mãi, giảm giá',      1, NOW(), NOW(), 0, NULL),
(202601006, 'EMPLOYEE',   'Nhân Viên',            'Quản lý nhân viên, ca làm việc, lương cơ bản',               1, NOW(), NOW(), 0, NULL),
(202601007, 'SALARY',     'Lương Nhân Viên',      'Quản lý bảng lương, tính toán lương, chi trả',               1, NOW(), NOW(), 0, NULL),
(202601008, 'CUSTOMER',   'Khách Hàng',           'Quản lý thông tin khách hàng, lịch sử mua hàng',             1, NOW(), NOW(), 0, NULL),
(202601009, 'INVOICE',    'Hóa Đơn',              'Quản lý hóa đơn, xuất hóa đơn điện tử',                     1, NOW(), NOW(), 0, NULL),
(202601010, 'REVENUE',    'Doanh Thu',            'Xem báo cáo doanh thu, lợi nhuận, chi phí',                  1, NOW(), NOW(), 0, NULL),
(202601011, 'USER',       'Người Dùng',           'Quản lý tài khoản người dùng, quyền truy cập',               1, NOW(), NOW(), 0, NULL),
(202601012, 'SHOP_INFO',  'Thông Tin Cửa Hàng',  'Cập nhật thông tin cửa hàng, cấu hình hệ thống',             1, NOW(), NOW(), 0, NULL),
(202601014, 'VENDOR',     'Nhà Cung Cấp',         'Quản lý nhà cung cấp và đơn đặt hàng nhập hàng',             1, NOW(), NOW(), 0, NULL),
(202601016, 'INVENTORY',  'Quản Lý Kho',          'Theo dõi tồn kho, quản lý nhập xuất, cảnh báo hết hàng',     1, NOW(), NOW(), 0, NULL),
(202601017, 'LOYALTY',    'Tích Điểm Khách Hàng', 'Quản lý chương trình tích điểm và khách hàng thân thiết',   1, NOW(), NOW(), 0, NULL);

-- 2.2 Roles (convenience store roles)
INSERT IGNORE INTO `roles` (`id`, `name`, `description`, `created_at`, `updated_at`, `deleted`, `deleted_at`) VALUES
(1, 'SHOP_OWNER',    'Chủ cửa hàng - Toàn quyền quản lý mọi chức năng',                       NOW(), NOW(), 0, NULL),
(2, 'MANAGER',       'Quản lý - Quản lý vận hành, nhân viên, báo cáo',                         NOW(), NOW(), 0, NULL),
(3, 'CASHIER',       'Thu ngân - Xử lý bán hàng, đơn hàng, khuyến mãi',                       NOW(), NOW(), 0, NULL),
(4, 'STOCK_KEEPER',  'Thủ kho - Quản lý hàng hóa, nhập kho, kiểm kho',                        NOW(), NOW(), 0, NULL);

-- 2.3 Users
-- Default password hash — CHANGE IMMEDIATELY after first login.
INSERT IGNORE INTO `users` (`id`, `username`, `email`, `password`, `full_name`, `active`, `account_non_locked`, `credentials_non_expired`, `account_non_expired`, `lang`, `failed_login_attempts`, `deleted`) VALUES
(1, 'Administrator', 'admin@tapha.local',
 '$2a$10$pyg6ud.T6WmFBtcsyBp2TujecrqKNifJZPmewv2aJDApOVZWxbbi6',
 'Quản Trị Viên', 1, 1, 1, 1, 'vi', 0, 0);

-- 2.4 Role-feature mappings
-- SHOP_OWNER: all features
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`) VALUES
(1, 202601001, NOW()), (1, 202601002, NOW()), (1, 202601003, NOW()),
(1, 202601004, NOW()), (1, 202601005, NOW()), (1, 202601006, NOW()),
(1, 202601007, NOW()), (1, 202601008, NOW()), (1, 202601009, NOW()),
(1, 202601010, NOW()), (1, 202601011, NOW()), (1, 202601012, NOW()),
(1, 202601014, NOW()), (1, 202601016, NOW()), (1, 202601017, NOW());

-- MANAGER: operational management (no user admin, no system config)
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`) VALUES
(2, 202601001, NOW()), -- DASHBOARD
(2, 202601002, NOW()), -- ORDER
(2, 202601003, NOW()), -- MY_WORK
(2, 202601004, NOW()), -- PRODUCT
(2, 202601005, NOW()), -- PROMOTION
(2, 202601006, NOW()), -- EMPLOYEE
(2, 202601007, NOW()), -- SALARY
(2, 202601008, NOW()), -- CUSTOMER
(2, 202601009, NOW()), -- INVOICE
(2, 202601010, NOW()), -- REVENUE
(2, 202601014, NOW()), -- VENDOR
(2, 202601016, NOW()), -- INVENTORY
(2, 202601017, NOW()); -- LOYALTY

-- CASHIER: POS sales operations
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`) VALUES
(3, 202601001, NOW()), -- DASHBOARD
(3, 202601002, NOW()), -- ORDER
(3, 202601003, NOW()), -- MY_WORK
(3, 202601005, NOW()), -- PROMOTION
(3, 202601008, NOW()), -- CUSTOMER
(3, 202601017, NOW()); -- LOYALTY

-- STOCK_KEEPER: inventory and purchasing
INSERT IGNORE INTO `role_features` (`role_id`, `feature_id`, `created_at`) VALUES
(4, 202601001, NOW()), -- DASHBOARD
(4, 202601003, NOW()), -- MY_WORK
(4, 202601004, NOW()), -- PRODUCT
(4, 202601014, NOW()), -- VENDOR
(4, 202601016, NOW()); -- INVENTORY

-- 2.5 User-role assignment
INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`) VALUES (1, 1);

-- 2.6 Shop info (convenience store)
INSERT INTO `shop_info`
    (`shop_name`, `address`, `company_name`, `default_tax_rate`, `phone`,
     `invoice_system`, `pos_mode`,
     `cash_denominations`)
VALUES
    ('Tạp Hóa Bình Dân',
     '123 Đường Nguyễn Trãi, Phường 3, Quận 5, TP.HCM',
     'Hộ kinh doanh Tạp Hóa Bình Dân',
     0.00,
     '0901234567',
     'MOCK',
     'STANDARD',
     '500,1000,2000,5000,10000,20000,50000,100000,200000,500000');

-- 2.7 Product types (18 standard types)
INSERT INTO `product_type` (`code`, `name`, `description`) VALUES
('FOOD',         'Thực phẩm',              'Thực phẩm và đồ ăn'),
('BEVERAGE',     'Đồ uống',                'Nước giải khát, bia, nước suối'),
('DRUG',         'Dược phẩm',              'Thuốc và sản phẩm dược'),
('CONVENIENCE',  'Hàng tiêu dùng',         'Hàng tiêu dùng thiết yếu'),
('BIKE',         'Xe đạp / Xe máy',        'Xe đạp và phụ tùng xe máy'),
('HARDWARE',     'Đồ sắt / Dụng cụ',      'Đồ sắt và dụng cụ'),
('CLOTHING',     'Quần áo / May mặc',      'Quần áo và phụ kiện'),
('ELECTRONICS',  'Điện tử',                'Thiết bị điện tử'),
('FURNITURE',    'Đồ nội thất',            'Nội thất gia đình'),
('BEAUTY',       'Làm đẹp / Chăm sóc cá nhân', 'Sản phẩm làm đẹp và vệ sinh cá nhân'),
('TOYS',         'Đồ chơi / Trò chơi',    'Đồ chơi và trò chơi'),
('BOOKS',        'Sách / Văn phòng phẩm', 'Sách và văn phòng phẩm'),
('SPORTS',       'Thể thao / Ngoài trời', 'Thiết bị thể thao'),
('AUTO_PARTS',   'Phụ tùng ô tô',         'Phụ tùng và phụ kiện ô tô'),
('APPLIANCES',   'Đồ gia dụng',            'Thiết bị gia dụng'),
('OFFICE',       'Văn phòng phẩm',         'Đồ dùng văn phòng'),
('PET',          'Thú cưng',              'Thức ăn và phụ kiện thú cưng'),
('HEALTH',       'Sức khỏe / Dinh dưỡng', 'Sản phẩm sức khỏe và dinh dưỡng')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.8 Categories (convenience store hierarchy)
INSERT INTO `category` (`id`, `name`, `parent_id`) VALUES
-- Top-level
(1,  'Đồ uống',                NULL),
(2,  'Thực phẩm',              NULL),
(3,  'Bánh kẹo & Snacks',      NULL),
(4,  'Vệ sinh cá nhân',        NULL),
(5,  'Đồ gia dụng',            NULL),
(6,  'Thuốc lá',               NULL),
(7,  'Sữa & Sản phẩm sữa',    NULL),
(8,  'Gia vị & Nước chấm',     NULL),
-- Sub-categories under Đồ uống
(9,  'Nước giải khát',         1),
(10, 'Nước suối / Nước tinh khiết', 1),
(11, 'Bia & Nước có cồn',      1),
(12, 'Trà & Cà phê đóng gói',  1),
(13, 'Nước tăng lực',          1),
-- Sub-categories under Thực phẩm
(14, 'Mì gói & Cháo gói',      2),
(15, 'Thực phẩm khô',          2),
-- Sub-categories under Bánh kẹo
(16, 'Bánh quy & Bánh ngọt',   3),
(17, 'Kẹo & Socola',           3),
(18, 'Snack khoai tây',        3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.9 Walk-in customer (required by system for anonymous sales)
INSERT IGNORE INTO `customers`
    (`id`, `name`, `phone`, `email`, `notes`, `deleted`)
VALUES
    (1, 'Khách lẻ', '0000000000', NULL,
     'Khách hàng lẻ - không có thông tin liên hệ', FALSE);

-- 2.10 Vendors (sample wholesalers)
INSERT IGNORE INTO `vendors`
    (`id`, `name`, `code`, `contact_name`, `phone`, `address`, `payment_terms`, `is_active`, `notes`)
VALUES
(1, 'Công ty TNHH Phân Phối Hoàng Long',  'HOANG-LONG',
   'Nguyễn Văn Long', '0281234567',
   '45 Bình Đông, Phường 14, Quận 8, TP.HCM',
   'NET_30', 1, 'Nhà phân phối đồ uống và hàng tiêu dùng'),
(2, 'Metro Cash & Carry Việt Nam',         'METRO-VN',
   'Phòng Kinh Doanh', '0281234599',
   '180 Cao Lỗ, Phường 4, Quận 8, TP.HCM',
   'IMMEDIATE', 1, 'Siêu thị bán buôn tổng hợp');

-- 2.11 Products (30 common convenience store items)
INSERT INTO `product` (`id`, `product_type_id`, `sku`, `name`, `description`, `price`, `cost_price`, `unit`, `vendor_id`, `status`) VALUES
-- Nước giải khát (product_type: BEVERAGE)
(1,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-001', 'Coca-Cola 330ml',           'Nước ngọt có ga lon 330ml',          12000,  8500, 'lon',    1, 'ACTIVE'),
(2,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-002', 'Pepsi 330ml',               'Nước ngọt có ga lon 330ml',          12000,  8500, 'lon',    1, 'ACTIVE'),
(3,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-003', '7-Up 330ml',                'Nước ngọt có ga lon 330ml',          11000,  7500, 'lon',    1, 'ACTIVE'),
(4,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-004', 'Red Bull 250ml',            'Nước tăng lực lon 250ml',            13000,  9500, 'lon',    1, 'ACTIVE'),
(5,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-005', 'Number One 330ml',          'Nước tăng lực lon 330ml',            10000,  7000, 'lon',    1, 'ACTIVE'),
(6,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-006', 'Nước suối Aqua 500ml',      'Nước tinh khiết chai 500ml',          6000,  3500, 'chai',   1, 'ACTIVE'),
(7,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-007', 'Nước suối La Vie 500ml',    'Nước khoáng thiên nhiên chai 500ml',  7000,  4500, 'chai',   1, 'ACTIVE'),
(8,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-008', 'Bia Tiger 330ml',           'Bia lon 330ml',                      18000, 13000, 'lon',    1, 'ACTIVE'),
(9,  (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-009', 'Bia Heineken 330ml',        'Bia lon 330ml',                      25000, 18000, 'lon',    1, 'ACTIVE'),
(10, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-010', 'Bia Saigon Đỏ 330ml',      'Bia lon 330ml',                      15000, 11000, 'lon',    1, 'ACTIVE'),
(11, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-011', 'Trà Olong Tea Plus 455ml',  'Trà Olong chai PET 455ml',           12000,  8000, 'chai',   1, 'ACTIVE'),
(12, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-012', 'Trà xanh 0 Độ 455ml',      'Trà xanh không đường chai 455ml',    12000,  8000, 'chai',   1, 'ACTIVE'),
(13, (SELECT id FROM product_type WHERE code='BEVERAGE'), 'BEV-013', 'Sting Dâu 330ml',           'Nước tăng lực hương dâu lon 330ml',  10000,  7000, 'lon',    1, 'ACTIVE'),
-- Mì gói & Thực phẩm (product_type: FOOD)
(14, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-001', 'Mì Hảo Hảo Tôm Chua Cay 75g',  'Mì ăn liền gói 75g',          7000,  4800, 'gói',    1, 'ACTIVE'),
(15, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-002', 'Mì 3 Miền Bò Hầm Rau 65g',     'Mì ăn liền gói 65g',          6000,  4000, 'gói',    1, 'ACTIVE'),
(16, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-003', 'Phở Bò Ăn Liền Vifon 65g',     'Phở ăn liền gói 65g',         8000,  5500, 'gói',    1, 'ACTIVE'),
(17, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-004', 'Cháo Thịt Bằm Vifon 50g',      'Cháo ăn liền gói 50g',       12000,  8500, 'gói',    1, 'ACTIVE'),
(18, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-005', 'Sữa TH True Milk 180ml',        'Sữa tươi tiệt trùng hộp',     8000,  5800, 'hộp',    2, 'ACTIVE'),
(19, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-006', 'Sữa Vinamilk UHT 180ml',        'Sữa tươi UHT hộp 180ml',      7500,  5200, 'hộp',    2, 'ACTIVE'),
(20, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-007', 'Nước mắm Nam Ngư 500ml',        'Nước mắm chai thuỷ tinh 500ml',18000,12500, 'chai',   2, 'ACTIVE'),
(21, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-008', 'Dầu ăn Neptune 500ml',          'Dầu thực vật chai 500ml',     42000, 31000, 'chai',   2, 'ACTIVE'),
(22, (SELECT id FROM product_type WHERE code='FOOD'),     'FOOD-009', 'Mì chính Ajinomoto 100g',       'Bột ngọt gói 100g',           12000,  8500, 'gói',    2, 'ACTIVE'),
-- Bánh kẹo & Snacks (product_type: CONVENIENCE)
(23, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-001','Bánh Oreo 97g',             'Bánh quy kem 97g',                   18000, 13000, 'gói',  2, 'ACTIVE'),
(24, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-002','Snack Poca Khoai Tây BBQ 68g','Snack khoai tây vị BBQ gói 68g',   12000,  8500, 'gói',  2, 'ACTIVE'),
(25, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-003','Kẹo Dừa Bến Tre 200g',      'Kẹo dừa truyền thống gói 200g',     25000, 18000, 'gói',  2, 'ACTIVE'),
(26, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-004','Bánh Kinh Đô Hương Vani',   'Bánh quy bơ hộp 150g',              22000, 15000, 'hộp',  2, 'ACTIVE'),
-- Vệ sinh cá nhân (product_type: BEAUTY)
(27, (SELECT id FROM product_type WHERE code='BEAUTY'),   'BEAU-001', 'Kem đánh răng Colgate 150g',   'Kem đánh răng bảo vệ toàn diện',   35000, 25000, 'tuýp',  1, 'ACTIVE'),
(28, (SELECT id FROM product_type WHERE code='BEAUTY'),   'BEAU-002', 'Dầu gội Clear Mát Lạnh 170ml','Dầu gội sạch gàu chai 170ml',      45000, 33000, 'chai',  1, 'ACTIVE'),
(29, (SELECT id FROM product_type WHERE code='BEAUTY'),   'BEAU-003', 'Xà phòng Lifebuoy 90g',       'Xà phòng kháng khuẩn 90g',         18000, 13000, 'bánh', 1, 'ACTIVE'),
(30, (SELECT id FROM product_type WHERE code='BEAUTY'),   'BEAU-004', 'Bàn chải Oral-B Classic',     'Bàn chải đánh răng lông mềm',      25000, 18000, 'cái',   1, 'ACTIVE'),
-- Đồ gia dụng (product_type: CONVENIENCE)
(31, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-005','Nước rửa chén Sunlight Chanh 500ml','Nước rửa chén chai 500ml', 22000, 16000, 'chai', 2, 'ACTIVE'),
(32, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-006','Bột giặt Omo Comfort 400g',  'Bột giặt thơm gói 400g',            32000, 24000, 'gói',  2, 'ACTIVE'),
(33, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-007','Túi nilon HDPE 30 cái',      'Túi đựng hàng 30x40cm, 30 cái/cuộn', 5000,  3000, 'cuộn', 2, 'ACTIVE'),
-- Thuốc lá
(34, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-008','Thuốc lá Esse Menthol',      'Thuốc lá Esse bạc hà 1 bao',        30000, 25000, 'bao',  2, 'ACTIVE'),
(35, (SELECT id FROM product_type WHERE code='CONVENIENCE'),'CONV-009','Thuốc lá 555 State Express',  'Thuốc lá 555 1 bao',                35000, 30000, 'bao',  2, 'ACTIVE');

-- 2.12 Product-category assignments
INSERT IGNORE INTO `product_category` (`product_id`, `category_id`) VALUES
-- Beverages → sub-categories
(1,9),(2,9),(3,9),                  -- Nước giải khát
(6,10),(7,10),                      -- Nước suối
(8,11),(9,11),(10,11),              -- Bia
(11,12),(12,12),                    -- Trà
(4,13),(5,13),(13,13),             -- Nước tăng lực
-- Food
(14,14),(15,14),(16,14),(17,14),   -- Mì gói / Cháo gói
(18,7),(19,7),                     -- Sữa
(20,8),(21,8),(22,8),              -- Gia vị
-- Snacks
(23,16),(24,18),(25,17),(26,16),   -- Bánh kẹo / Snacks
-- Personal care → category 4
(27,4),(28,4),(29,4),(30,4),
-- Household → category 5
(31,5),(32,5),(33,5),
-- Tobacco → category 6
(34,6),(35,6);

-- 2.13 Inventory (initial stock for all products)
-- quantity_in_stock: starting stock; reorder_level: trigger reorder; reorder_quantity: qty to order
INSERT INTO `inventory`
    (`product_id`, `quantity_in_stock`, `reorder_level`, `reorder_quantity`,
     `unit_cost`, `warehouse_location`, `status`, `inventory_type`, `last_restock_date`)
VALUES
-- Beverages
(1,  120, 24,  96, 8500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(2,  120, 24,  96, 8500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(3,  100, 24,  96, 7500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(4,   60, 12,  48, 9500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(5,   80, 24,  96, 7000,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(6,  200, 48, 144, 3500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(7,  200, 48, 144, 4500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(8,   72, 24,  72, 13000, 'Tủ lạnh',   'ACTIVE', 'RETAIL', NOW()),
(9,   48, 12,  48, 18000, 'Tủ lạnh',   'ACTIVE', 'RETAIL', NOW()),
(10,  72, 24,  72, 11000, 'Tủ lạnh',   'ACTIVE', 'RETAIL', NOW()),
(11, 100, 24,  96, 8000,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(12, 100, 24,  96, 8000,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(13,  80, 24,  96, 7000,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
-- Food
(14, 200, 50, 150, 4800,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(15, 200, 50, 150, 4000,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(16, 150, 30, 100, 5500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(17, 100, 30,  80, 8500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(18,  60, 24,  48, 5800,  'Tủ lạnh',   'ACTIVE', 'RETAIL', NOW()),
(19,  60, 24,  48, 5200,  'Tủ lạnh',   'ACTIVE', 'RETAIL', NOW()),
(20,  50, 12,  36, 12500, 'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(21,  30, 10,  30, 31000, 'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(22,  80, 20,  60, 8500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
-- Snacks
(23,  80, 20,  60, 13000, 'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(24, 100, 24,  72, 8500,  'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(25,  60, 15,  45, 18000, 'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
(26,  50, 12,  36, 15000, 'Kho chính', 'ACTIVE', 'RETAIL', NOW()),
-- Personal care
(27,  40, 10,  30, 25000, 'Kệ A-01',   'ACTIVE', 'RETAIL', NOW()),
(28,  30, 10,  30, 33000, 'Kệ A-01',   'ACTIVE', 'RETAIL', NOW()),
(29,  60, 15,  45, 13000, 'Kệ A-01',   'ACTIVE', 'RETAIL', NOW()),
(30,  40, 10,  30, 18000, 'Kệ A-01',   'ACTIVE', 'RETAIL', NOW()),
-- Household
(31,  40, 10,  30, 16000, 'Kệ B-01',   'ACTIVE', 'RETAIL', NOW()),
(32,  30,  8,  24, 24000, 'Kệ B-01',   'ACTIVE', 'RETAIL', NOW()),
(33, 100, 20,  60,  3000, 'Kệ B-02',   'ACTIVE', 'RETAIL', NOW()),
-- Tobacco
(34,  50, 10,  30, 25000, 'Quầy tính tiền', 'ACTIVE', 'RETAIL', NOW()),
(35,  30,  6,  18, 30000, 'Quầy tính tiền', 'ACTIVE', 'RETAIL', NOW());

-- 2.14 Loyalty program (default program)
INSERT INTO `loyalty_programs`
    (`points_per_amount`, `amount_per_points`, `redemption_points_per_discount`,
     `redemption_discount_amount`, `min_redemption_points`, `is_active`)
VALUES (1, 10000, 100, 10000.00, 100, 1);

-- 2.15 Loyalty tiers
INSERT INTO `loyalty_tiers` (`name`, `min_spend`, `points_multiplier`, `color`, `description`, `sort_order`) VALUES
('Đồng',      0,          1.00, '#CD7F32', 'Thành viên cơ bản',              1),
('Bạc',       2000000,    1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',        2),
('Vàng',      10000000,   1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND',       3),
('Kim cương', 50000000,   2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND',       4);

-- 2.16 Print templates (default receipt)
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

-- 2.17 Attribute groups and definitions for CONVENIENCE product type
-- (Basic Information)
INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'basic_info', 'Thông tin cơ bản', 1 FROM `product_type` WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 1;

INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'storage_handling', 'Bảo quản & Xử lý', 2 FROM `product_type` WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 2;

INSERT INTO `attribute_group` (`product_type_id`, `code`, `name`, `display_order`)
SELECT id, 'supplier_info', 'Thông tin nhà cung cấp', 3 FROM `product_type` WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 3;

-- Attribute definitions for CONVENIENCE
INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'brand', 'Thương hiệu / Nhà sản xuất', 'STRING', FALSE, TRUE, TRUE, 1
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'item_category', 'Danh mục hàng hóa', 'STRING', TRUE, TRUE, TRUE, 2
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'package_size', 'Dung tích / Trọng lượng', 'STRING', FALSE, TRUE, TRUE, 3
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'country_of_origin', 'Xuất xứ', 'STRING', FALSE, TRUE, TRUE, 4
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'barcode_upc', 'Mã vạch / Barcode', 'STRING', FALSE, TRUE, FALSE, 5
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'basic_info'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'expiry_date', 'Hạn sử dụng', 'DATE', FALSE, FALSE, TRUE, 1
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO `attribute_definition` (`product_type_id`, `attribute_group_id`, `code`, `name`, `data_type`, `required`, `searchable`, `filterable`, `display_order`)
SELECT pt.id, ag.id, 'storage_requirement', 'Điều kiện bảo quản', 'STRING', FALSE, FALSE, TRUE, 2
FROM `product_type` pt JOIN `attribute_group` ag ON ag.product_type_id = pt.id AND ag.code = 'storage_handling'
WHERE pt.code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.18 Banks (complete list of Vietnamese banks with VietQR BIN codes)
INSERT IGNORE INTO `banks` (`code`, `bin`, `name`, `short_name`, `sort_order`) VALUES
-- State-owned / state-capital banks
('VCB',   '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam',                              'Vietcombank',         1),
('CTG',   '970415', 'Ngân hàng TMCP Công thương Việt Nam',                               'VietinBank',          2),
('BID',   '970418', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam',                      'BIDV',                3),
('AGR',   '970405', 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam',            'Agribank',            4),
-- Top private joint-stock banks
('MBB',   '970422', 'Ngân hàng TMCP Quân đội',                                           'MB Bank',             5),
('TCB',   '970407', 'Ngân hàng TMCP Kỹ thương Việt Nam',                                 'Techcombank',         6),
('VPB',   '970432', 'Ngân hàng TMCP Việt Nam Thịnh Vượng',                               'VPBank',              7),
('ACB',   '970416', 'Ngân hàng TMCP Á Châu',                                             'ACB',                 8),
('STB',   '970403', 'Ngân hàng TMCP Sài Gòn Thương Tín',                                 'Sacombank',           9),
('TPB',   '970423', 'Ngân hàng TMCP Tiên Phong',                                         'TPBank',             10),
('HDB',   '970437', 'Ngân hàng TMCP Phát triển TP.HCM',                                  'HDBank',             11),
('VIB',   '970441', 'Ngân hàng TMCP Quốc tế Việt Nam',                                   'VIB',                12),
('SHB',   '970443', 'Ngân hàng TMCP Sài Gòn - Hà Nội',                                  'SHB',                13),
('EIB',   '970431', 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam',                            'Eximbank',           14),
('LPB',   '970449', 'Ngân hàng TMCP Bưu điện Liên Việt',                                 'LienVietPostBank',   15),
('MSB',   '970426', 'Ngân hàng TMCP Hàng Hải Việt Nam',                                  'MSB',                16),
('OCB',   '970448', 'Ngân hàng TMCP Phương Đông',                                        'OCB',                17),
('SSB',   '970440', 'Ngân hàng TMCP Đông Nam Á',                                         'SeABank',            18),
('ABB',   '970425', 'Ngân hàng TMCP An Bình',                                             'ABBank',             19),
-- Smaller private banks
('BAB',   '970409', 'Ngân hàng TMCP Bắc Á',                                              'BacABank',           20),
('BVB',   '970454', 'Ngân hàng TMCP Bản Việt',                                           'BVBank',             21),
('KLB',   '970462', 'Ngân hàng TMCP Kiên Long',                                          'KienLongBank',       22),
('NAB',   '970428', 'Ngân hàng TMCP Nam Á',                                              'NamABank',           23),
('NCB',   '970419', 'Ngân hàng TMCP Quốc Dân',                                           'NCB',                24),
('PGB',   '970430', 'Ngân hàng TMCP Xăng dầu Petrolimex',                                'PGBank',             25),
('PVCB',  '970452', 'Ngân hàng TMCP Đại Chúng Việt Nam',                                 'PVcomBank',          26),
('SGB',   '970400', 'Ngân hàng TMCP Sài Gòn Công Thương',                                'Saigonbank',         27),
('VBB',   '970433', 'Ngân hàng TMCP Việt Nam Thương Tín',                                'VietBank',           28),
('BVK',   '970438', 'Ngân hàng TMCP Bảo Việt',                                           'BaoVietBank',        29),
('GPB',   '970408', 'Ngân hàng TMCP Dầu khí Toàn Cầu',                                   'GPBank',             30),
('OJB',   '970414', 'Ngân hàng Thương mại TNHH MTV Đại Dương',                           'OceanBank',          31),
('DAB',   '970406', 'Ngân hàng TMCP Đông Á',                                             'DongABank',          32),
('CBB',   '970444', 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam',                   'CBBank',             33),
-- Digital / neobanks
('CAKE',  '546034', 'CAKE by VPBank',                                                     'CAKE',               40),
('UBANK', '546035', 'Ubank by VPBank',                                                    'Ubank',              41),
('TIMO',  '963388', 'Timo by BVBank',                                                     'Timo',               42),
('TNEX',  '533948', 'TNEX by MSB',                                                        'TNEX',               43),
-- Foreign banks
('HSBC',  '458761', 'Ngân hàng TNHH MTV HSBC Việt Nam',                                  'HSBC Vietnam',       50),
('SC',    '970410', 'Ngân hàng TNHH MTV Standard Chartered Việt Nam',                     'Standard Chartered',  51),
('SHIN',  '970424', 'Ngân hàng TNHH MTV Shinhan Việt Nam',                                'Shinhan Vietnam',    52),
('WOORI', '970457', 'Ngân hàng TNHH MTV Woori Việt Nam',                                  'Woori Vietnam',      53),
('UOB',   '970458', 'Ngân hàng UOB Việt Nam',                                             'UOB Vietnam',        54),
('HLB',   '970442', 'Ngân hàng TNHH MTV Hong Leong Việt Nam',                             'Hong Leong Vietnam', 55),
('PBB',   '970439', 'Ngân hàng TNHH MTV Public Việt Nam',                                 'Public Bank Vietnam',56),
('IBK',   '970455', 'Ngân hàng Công nghiệp Hàn Quốc - IBK Việt Nam',                     'IBK Vietnam',        57),
('KEXIM', '668888', 'Ngân hàng Xuất nhập khẩu Hàn Quốc - KEXIM Việt Nam',               'KEXIM Vietnam',      58);

SET FOREIGN_KEY_CHECKS = 1;
