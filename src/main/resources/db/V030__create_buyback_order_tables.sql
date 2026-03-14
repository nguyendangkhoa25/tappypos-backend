-- Market prices table (commodity prices used as basis for buyback valuation)
CREATE TABLE `market_prices` (
  `id`          bigint         NOT NULL AUTO_INCREMENT,
  `name`        varchar(100)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Commodity name (e.g. Vàng 24K, Bạc, Đá Quý)',
  `unit`        varchar(20)    COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Weight/measure unit (gram, chỉ, lượng, carat)',
  `buy_price`   decimal(15,2)  NOT NULL DEFAULT '0.00'   COMMENT 'Price per unit when buying from customer',
  `sell_price`  decimal(15,2)           DEFAULT NULL      COMMENT 'Price per unit when selling to customer',
  `is_active`   tinyint(1)     NOT NULL DEFAULT '1',
  `notes`       varchar(500)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order`  int            NOT NULL DEFAULT '999',
  `created_at`  timestamp      NULL     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  timestamp      NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)     NOT NULL DEFAULT '0',
  `deleted_at`  timestamp      NULL     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_active`     (`is_active`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_deleted`    (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default commodity list for jewelry shops
INSERT INTO `market_prices` (`name`, `unit`, `buy_price`, `sell_price`, `is_active`, `sort_order`) VALUES
('Vàng 24K (999)',  'chỉ', 8500000, 8700000, 1, 1),
('Vàng 22K (916)',  'chỉ', 7800000, 8000000, 1, 2),
('Vàng 18K (750)',  'chỉ', 6400000, 6600000, 1, 3),
('Vàng 14K (585)',  'chỉ', 5000000, 5100000, 1, 4),
('Vàng 10K (417)',  'chỉ', 3500000, 3600000, 1, 5),
('Bạc (925)',       'gram',   15000,   18000, 1, 6),
('Bạc (999)',       'gram',   16000,   19000, 1, 7),
('Đá Ruby',        'carat',  500000,  800000, 1, 8),
('Đá Sapphire',    'carat',  400000,  700000, 1, 9),
('Đá Emerald',     'carat',  600000,  900000, 1, 10),
('Kim Cương',      'carat', 5000000, 8000000, 1, 11);

-- Buyback / repurchase orders
CREATE TABLE `buyback_orders` (
  `id`             bigint         NOT NULL AUTO_INCREMENT,
  `order_number`   varchar(30)    COLLATE utf8mb4_unicode_ci NOT NULL,
  `type`           varchar(20)    COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BUY | EXCHANGE',
  `status`         varchar(20)    COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPLETED | CANCELLED',
  `customer_id`    bigint                  DEFAULT NULL,
  `customer_name`  varchar(100)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_phone` varchar(20)    COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` varchar(20)    COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CASH',
  `buy_total`      decimal(15,2)  NOT NULL DEFAULT '0.00',
  `sale_total`     decimal(15,2)  NOT NULL DEFAULT '0.00',
  `net_amount`     decimal(15,2)  NOT NULL DEFAULT '0.00',
  `notes`          varchar(500)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by`     varchar(100)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at`   timestamp      NULL DEFAULT NULL,
  `completed_by`   varchar(100)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at`   timestamp      NULL DEFAULT NULL,
  `cancelled_by`   varchar(100)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at`     timestamp      NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     timestamp      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        tinyint(1)     NOT NULL DEFAULT '0',
  `deleted_at`     timestamp      NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_number` (`order_number`),
  KEY `idx_type`        (`type`),
  KEY `idx_status`      (`status`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_created_at`  (`created_at`),
  KEY `idx_deleted`     (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Line items — holds both BUY items (commodities) and SALE items (exchange goods)
CREATE TABLE `buyback_order_items` (
  `id`               bigint         NOT NULL AUTO_INCREMENT,
  `buyback_order_id` bigint         NOT NULL,
  `item_type`        varchar(10)    COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BUY | SALE',
  -- BUY item fields
  `commodity_id`     bigint                  DEFAULT NULL,
  `commodity_name`   varchar(100)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit`             varchar(20)    COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weight`           decimal(10,3)           DEFAULT NULL,
  `condition_type`   varchar(20)    COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'NEW | USED | SCRAP',
  `price_per_unit`   decimal(15,2)           DEFAULT NULL,
  -- SALE item fields
  `product_name`     varchar(255)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity`         int                     DEFAULT NULL,
  `unit_price`       decimal(15,2)           DEFAULT NULL,
  -- Common
  `total_price`      decimal(15,2)  NOT NULL DEFAULT '0.00',
  `notes`            varchar(500)   COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at`       timestamp      NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       timestamp      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          tinyint(1)     NOT NULL DEFAULT '0',
  `deleted_at`       timestamp      NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id`  (`buyback_order_id`),
  KEY `idx_item_type` (`item_type`),
  CONSTRAINT `fk_boi_order` FOREIGN KEY (`buyback_order_id`) REFERENCES `buyback_orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
