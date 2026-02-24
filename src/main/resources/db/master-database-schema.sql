
-- Step 1, Create employees table
CREATE TABLE `employees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `hire_date` date DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE','ON_LEAVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `description` text COLLATE utf8mb4_unicode_ci,
  `base_salary` decimal(10,2) NOT NULL DEFAULT '0.00',
  `total_earned` decimal(10,2) DEFAULT '0.00',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_phone` (`phone`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=79202600001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Step 2, Create features table
CREATE TABLE `features` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Feature code/identifier (e.g., DASHBOARD, ORDER)',
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Display name for UI (e.g., Dashboard)',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Detailed description of the feature',
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Is feature enabled',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_name` (`name`),
  KEY `idx_active` (`active`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=202601014 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Step 3, Create users table
CREATE TABLE `users` (
     `id` bigint NOT NULL AUTO_INCREMENT,
     `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
     `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
     `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
     `require_action` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
     `full_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
     `active` tinyint(1) NOT NULL DEFAULT '1',
     `account_non_locked` tinyint(1) NOT NULL DEFAULT '1',
     `credentials_non_expired` tinyint(1) NOT NULL DEFAULT '1',
     `account_non_expired` tinyint(1) NOT NULL DEFAULT '1',
     `notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
     `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
     `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `deleted` tinyint(1) NOT NULL DEFAULT '0',
     `deleted_at` timestamp NULL DEFAULT NULL,
     `avatar` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Base64 encoded user avatar/profile picture',
     `color_preference` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User color preference for UI theme',
     `lang` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'vi' COMMENT 'User language preference (vi, en, etc.)',
     PRIMARY KEY (`id`),
     UNIQUE KEY `username` (`username`),
     KEY `idx_username` (`username`),
     KEY `idx_active` (`active`),
     KEY `idx_users_active` (`active`,`username`)
) ENGINE=InnoDB AUTO_INCREMENT=79260002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 4, Create roles table
CREATE TABLE `roles` (
     `id` bigint NOT NULL AUTO_INCREMENT,
     `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
     `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
     `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
     `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `deleted` tinyint(1) NOT NULL DEFAULT '0',
     `deleted_at` timestamp NULL DEFAULT NULL,
     PRIMARY KEY (`id`),
     UNIQUE KEY `name` (`name`),
     KEY `idx_name` (`name`),
     KEY `idx_deleted` (`deleted`),
     KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=202600002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 5, Create role_features table
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiry_date` bigint NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` bigint NOT NULL,
  `updated_at` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `idx_token` (`token`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_active` (`active`),
  CONSTRAINT `refresh_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 6, Create role_features table
CREATE TABLE `role_features` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL COMMENT 'Reference to roles table',
  `feature_id` bigint NOT NULL COMMENT 'Reference to features table',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_feature` (`role_id`,`feature_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_feature_id` (`feature_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_role_features_feature_id` FOREIGN KEY (`feature_id`) REFERENCES `features` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_features_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20260003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Maps roles to features for role-based access control';

-- Step 7, Create tenants table
CREATE TABLE `tenants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `db_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `expiration_date` date DEFAULT NULL,
  `max_users` int DEFAULT NULL,
  `features` text COLLATE utf8mb4_unicode_ci,
  `subscription_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person_email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person_zalo_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` bigint NOT NULL,
  `updated_at` bigint NOT NULL,
  `active_at` bigint DEFAULT NULL,
  `active_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_id` (`tenant_id`),
  UNIQUE KEY `db_name` (`db_name`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_active` (`active`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 8, Create user_roles table
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

