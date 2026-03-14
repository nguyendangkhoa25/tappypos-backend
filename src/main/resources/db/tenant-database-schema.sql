
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

SET NAMES utf8mb4;


CREATE TABLE `api_audit_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Unique trace ID for tracking requests across systems',
  `api_endpoint` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API endpoint URL that was called',
  `http_method` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HTTP method (GET, POST, PUT, DELETE, etc.)',
  `request_body` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Request payload in JSON format',
  `request_headers` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Request headers in JSON format',
  `response_body` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Response payload in JSON format',
  `response_headers` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Response headers in JSON format',
  `response_status` int DEFAULT NULL COMMENT 'HTTP response status code',
  `request_size` bigint DEFAULT NULL COMMENT 'Size of request in bytes',
  `response_size` bigint DEFAULT NULL COMMENT 'Size of response in bytes',
  `execution_time_ms` bigint DEFAULT NULL COMMENT 'Execution time in milliseconds',
  `error_message` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Error message if request failed',
  `exception_stack_trace` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Exception stack trace for debugging',
  `user_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ID of the user who made the request',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP address of the client',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Status: SUCCESS, FAILURE, PARTIAL_FAILURE',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the log was created',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Optional description or notes',
  PRIMARY KEY (`log_id`),
  KEY `idx_api_endpoint` (`api_endpoint`(255)),
  KEY `idx_method` (`http_method`),
  KEY `idx_timestamp` (`created_at`),
  KEY `idx_status` (`response_status`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `zalo_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `facebook_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preferred_services` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `allergies_or_sensitivities` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hair_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `special_requests` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `gender` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card_issued_date` date DEFAULT NULL,
  `id_card_issued_place` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `permanent_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone` (`phone`),
  UNIQUE KEY `uq_id_card_number` (`id_card_number`),
  KEY `idx_phone` (`phone`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=100202600001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `orders`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `status` enum('PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `payment_method` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_paid` decimal(10,2) DEFAULT NULL,
  `change_amount` decimal(10,2) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `discount_amount` decimal(10,2) DEFAULT '0.00',
  `tax_percentage` decimal(5,2) DEFAULT '0.00',
  `tax_amount` decimal(10,2) DEFAULT '0.00',
  `commission_amount` decimal(10,2) DEFAULT '0.00',
  `invoice_id` bigint DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_number` (`order_number`),
  KEY `idx_status` (`status`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_deleted_at` (`deleted_at`),
  KEY `idx_invoice_id` (`invoice_id`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39202600017 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `order_items`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
   `id` bigint NOT NULL AUTO_INCREMENT,
   `order_id` bigint NOT NULL,
   `product_id` bigint NOT NULL,
   `product_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
   `quantity` int NOT NULL,
   `unit_price` decimal(10,2) NOT NULL,
   `amount` decimal(10,2) NOT NULL,
   `status` enum('PENDING','IN_PROGRESS','COMPLETED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
   `tax_percentage` decimal(5,2) DEFAULT '0.00',
   `tax_amount` decimal(10,2) DEFAULT '0.00',
   `commission_rate` decimal(5,2) DEFAULT '0.00',
   `commission_amount` decimal(10,2) DEFAULT '0.00',
   `amount_before_tax` decimal(10,2) DEFAULT '0.00',
   `assigned_employee_id` bigint DEFAULT NULL,
   `deleted` tinyint(1) NOT NULL DEFAULT '0',
   `completed_at` datetime DEFAULT NULL,
   `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
   `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   `deleted_at` timestamp NULL DEFAULT NULL,
   `included_in_salary_id` bigint DEFAULT NULL,
   `is_salary_calculated` tinyint(1) NOT NULL DEFAULT '0',
   PRIMARY KEY (`id`),
   KEY `idx_order_id` (`order_id`),
   KEY `idx_status` (`status`),
   KEY `idx_assigned_employee` (`assigned_employee_id`),
   KEY `idx_deleted_at` (`deleted_at`),
   CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invoices`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoices` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL,
    `invoice_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `invoice_series` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `total_amount` decimal(10,2) NOT NULL,
    `tax` decimal(10,2) DEFAULT '0.00',
    `status` enum('DRAFT','COMPLETED','FAILED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
    `external_invoice_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `external_sync_at` timestamp NULL DEFAULT NULL,
    `notes` text COLLATE utf8mb4_unicode_ci,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` timestamp NULL DEFAULT NULL,
    `issued_date` datetime DEFAULT NULL,
    `total_amount_without_tax` decimal(19,2) NOT NULL DEFAULT '0.00',
    `tax_amount` decimal(19,2) NOT NULL DEFAULT '0.00',
    `tax_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
    `payment_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `invoice_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `currency_code` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT 'VND',
    `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `deleted` tinyint(1) NOT NULL DEFAULT '0',
    `buyer_id` bigint DEFAULT NULL,
    `transaction_uuid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `invoice_number` (`invoice_number`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_invoice_number` (`invoice_number`),
    KEY `idx_deleted_at` (`deleted_at`),
    KEY `fk_invoices_buyer` (`buyer_id`),
    KEY `idx_external_invoice_id` (`external_invoice_id`),
    KEY `idx_deleted` (`deleted`),
    CONSTRAINT `fk_invoices_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `invoice_buyers` (`id`) ON DELETE SET NULL,
    CONSTRAINT `invoices_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21202600005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `invoice_buyers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_buyers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint DEFAULT NULL,
  `buyer_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_legal_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_tax_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_phone_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_bank_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_bank_account` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buyer_id_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_visiting_guest` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_buyer_tax_code` (`buyer_tax_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5520260005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invoice_items`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  `line_number` int DEFAULT NULL,
  `order_item_id` bigint DEFAULT NULL,
  `service_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_price` decimal(19,2) NOT NULL,
  `quantity` decimal(19,2) NOT NULL,
  `discount` decimal(19,2) DEFAULT '0.00',
  `total_amount_without_tax` decimal(19,2) NOT NULL,
  `tax_percentage` decimal(5,2) DEFAULT '0.00',
  `tax_amount` decimal(19,2) DEFAULT '0.00',
  `total_amount_with_tax` decimal(19,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_id` (`invoice_id`),
  KEY `idx_line_number` (`line_number`),
  KEY `idx_order_item_id` (`order_item_id`),
  CONSTRAINT `invoice_items_ibfk_1` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE,
  CONSTRAINT `check_invoice_item_amount` CHECK ((`total_amount_with_tax` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=5820260005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=36202600007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `refresh_tokens`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=202600001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
--
-- Table structure for table `role_features`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=20260045 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Maps roles to features for role-based access control';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
      `user_id` bigint NOT NULL,
      `role_id` bigint NOT NULL,
      PRIMARY KEY (`user_id`,`role_id`),
      KEY `idx_user_id` (`user_id`),
      KEY `idx_role_id` (`role_id`),
      CONSTRAINT `user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
      CONSTRAINT `user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inventory`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inventory_movement`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_movement` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shop_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_info` (
     `id` bigint NOT NULL AUTO_INCREMENT,
     `shop_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Tiệm tóc của tôi',
     `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `company_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `default_tax_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
     `e_invoice_username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `e_invoice_password` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `e_invoice_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `supplier_tax_code` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
     `invoice_vendor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `website` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '',
     `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
     `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `deleted` tinyint(1) NOT NULL DEFAULT '0',
     `deleted_at` timestamp NULL DEFAULT NULL,
     `template_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
     `invoice_system` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'S-INVOICE' COMMENT 'Invoice system type: S-INVOICE, M-INVOICE, MOCK',
     `invoice_series` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Invoice series for S-Invoice (e.g., C22TAA)',
     PRIMARY KEY (`id`),
     KEY `idx_deleted` (`deleted`),
     KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3920260102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE TABLE IF NOT EXISTS `print_templates` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `template_type` VARCHAR(50)  NOT NULL,
    `name`          VARCHAR(100) NOT NULL,
    `config_json`   TEXT         NOT NULL,
    `is_default`    TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    `deleted_at`    TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_print_templates_type_name` (`template_type`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

