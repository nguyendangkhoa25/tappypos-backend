-- ============================================================
-- MASTER DATABASE — DDL
-- Database: retail-platform-master
-- Run this on a fresh master database before inserting data.
-- All statements use IF NOT EXISTS — safe to re-run.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ── 1. features ───────────────────────────────────────────────
-- Defines every feature flag available across the platform.
-- The intersection of tenant features + role features is embedded in the JWT.
CREATE TABLE IF NOT EXISTS `features` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Feature code (e.g. DASHBOARD, ORDER)',
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
) ENGINE=InnoDB AUTO_INCREMENT=202601020 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 2. roles ──────────────────────────────────────────────────
-- Master-level roles only (MASTER_TENANT, VENDOR_ADMIN).
-- Tenant roles (SHOP_OWNER, MANAGER, etc.) live in each tenant DB.
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
) ENGINE=InnoDB AUTO_INCREMENT=202600003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. users ──────────────────────────────────────────────────
-- Master-level admin accounts only.
CREATE TABLE IF NOT EXISTS `users` (
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
    `username`                VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `email`                   VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `password`                VARCHAR(255) NOT NULL COLLATE utf8mb4_unicode_ci,
    `require_action`          VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `full_name`               VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `active`                  TINYINT(1)   NOT NULL DEFAULT 1,
    `account_non_locked`      TINYINT(1)   NOT NULL DEFAULT 1,
    `credentials_non_expired` TINYINT(1)   NOT NULL DEFAULT 1,
    `account_non_expired`     TINYINT(1)   NOT NULL DEFAULT 1,
    `notes`                   VARCHAR(255)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `failed_login_attempts`   INT          NOT NULL DEFAULT 0,
    `avatar`                  LONGTEXT              COLLATE utf8mb4_unicode_ci COMMENT 'Base64 encoded avatar',
    `color_preference`        VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `lang`                    VARCHAR(10)           COLLATE utf8mb4_unicode_ci DEFAULT 'vi',
    `created_at`              TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`              TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`              TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username`  (`username`),
    KEY `idx_username`        (`username`),
    KEY `idx_active`          (`active`),
    KEY `idx_users_active`    (`active`, `username`)
) ENGINE=InnoDB AUTO_INCREMENT=79260002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. user_roles ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 5. role_features ──────────────────────────────────────────
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
) ENGINE=InnoDB AUTO_INCREMENT=20260010 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Maps master roles to platform feature flags';

-- ── 6. refresh_tokens ─────────────────────────────────────────
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

-- ── 7. tenants ────────────────────────────────────────────────
-- One row per registered shop. Each row maps to a dedicated database.
CREATE TABLE IF NOT EXISTS `tenants` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`             VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `name`                  VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `db_name`               VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `active`                TINYINT(1)   NOT NULL DEFAULT 1,
    `expiration_date`       DATE                  DEFAULT NULL,
    `max_users`             INT                   DEFAULT NULL,
    `features`              TEXT                  COLLATE utf8mb4_unicode_ci COMMENT 'Comma-separated feature codes granted to this tenant',
    `subscription_type`     VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `shop_type`             VARCHAR(30)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'CONVENIENCE, PAWN, PHARMACY, RESTAURANT, etc.',
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
    `vendor_id`             BIGINT                DEFAULT NULL COMMENT 'FK to vendors.id — the VENDOR_ADMIN who manages this shop; NULL = managed directly by master admin',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`),
    UNIQUE KEY `uk_db_name`   (`db_name`),
    KEY `idx_tenant_id`       (`tenant_id`),
    KEY `idx_active`          (`active`),
    KEY `idx_vendor_id`       (`vendor_id`),
    CONSTRAINT `fk_tenant_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=20260401 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 8. active_sessions ────────────────────────────────────────
-- Enforces single-device login per master user.
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
  COMMENT='One active session per master user. Replaced on force-login.';

-- ── 9. user_feedback ──────────────────────────────────────────
-- Stores feedback/suggestions submitted by shop users from any tenant.
-- Routed here (master DB) so admins see all feedback in one place.
CREATE TABLE IF NOT EXISTS `user_feedback` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`   VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `username`    VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `type`        VARCHAR(20)  NOT NULL COLLATE utf8mb4_unicode_ci,
    `title`       VARCHAR(200) NOT NULL COLLATE utf8mb4_unicode_ci,
    `content`     TEXT         NOT NULL COLLATE utf8mb4_unicode_ci,
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `admin_note`  VARCHAR(1000)         COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `resolved_at` DATETIME(6)           DEFAULT NULL,
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6)           DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`  DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_feedback_tenant`   (`tenant_id`),
    INDEX `idx_feedback_username` (`username`),
    INDEX `idx_feedback_status`   (`status`),
    INDEX `idx_feedback_deleted`  (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 10. notifications ─────────────────────────────────────────
-- Notifications for master admin users: subscription expiry,
-- feedback alerts, and system-level events.
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Target username',
    `title`          VARCHAR(200) NOT NULL COLLATE utf8mb4_unicode_ci,
    `message`        TEXT                  COLLATE utf8mb4_unicode_ci,
    `type`           VARCHAR(30)  NOT NULL DEFAULT 'INFO' COMMENT 'SYSTEM, ORDER, ANNOUNCEMENT, LOW_STOCK, INFO, BILLING',
    `reference_type` VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'E.g. TENANT, FEEDBACK',
    `reference_id`   BIGINT                DEFAULT NULL,
    `is_read`        TINYINT(1)   NOT NULL DEFAULT 0,
    `read_at`        DATETIME(6)           DEFAULT NULL,
    `created_by`     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    `created_at`     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`     DATETIME(6)           DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_notifications_user_read` (`user_id`, `is_read`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 12. vendors ───────────────────────────────────────────────
-- Distributor/vendor admin organizations at master level.
-- The VENDOR_ADMIN user assigned to each vendor is tracked via vendors.user_id.
CREATE TABLE IF NOT EXISTS `vendors` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(100) NOT NULL COLLATE utf8mb4_unicode_ci,
    `contact_email` VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contact_phone` VARCHAR(20)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `notes`         TEXT                  COLLATE utf8mb4_unicode_ci,
    `active`        TINYINT(1)   NOT NULL DEFAULT 1,
    `user_id`       BIGINT                DEFAULT NULL COMMENT 'FK to users.id — the VENDOR_ADMIN user assigned to this vendor',
    `created_at`    TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`    TIMESTAMP    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_active`  (`active`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 13. activity_log ──────────────────────────────────────────
-- Records business-level actions performed by master/vendor admin users.
CREATE TABLE IF NOT EXISTS `activity_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `actor_username` VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Username of who performed the action',
    `actor_full_name` VARCHAR(100)         COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Display name at time of action',
    `action`         VARCHAR(50)  NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'ActivityAction enum value',
    `target_type`    VARCHAR(50)           COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'e.g. USER, TENANT, VENDOR',
    `target_id`      VARCHAR(100)          COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ID or reference of the target',
    `description`    VARCHAR(500) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT 'Human-readable description',
    `ip_address`     VARCHAR(45)           COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_activity_actor`      (`actor_username`),
    KEY `idx_activity_action`     (`action`),
    KEY `idx_activity_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Activity log for master and vendor admin users';

SET FOREIGN_KEY_CHECKS = 1;
