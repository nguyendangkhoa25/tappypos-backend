CREATE TABLE `bank_accounts` (
  `id`              bigint         NOT NULL AUTO_INCREMENT,
  `bank_bin`        varchar(20)    COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'VietQR BIN code',
  `bank_code`       varchar(20)    COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Bank short code (e.g. VCB)',
  `bank_name`       varchar(255)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Full bank name',
  `bank_short_name` varchar(100)   COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Display name (e.g. Vietcombank)',
  `account_number`  varchar(50)    COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_name`    varchar(255)   COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Account holder name (uppercase)',
  `is_default`      tinyint(1)     NOT NULL DEFAULT 0,
  `created_at`      datetime(6)    DEFAULT NULL,
  `updated_at`      datetime(6)    DEFAULT NULL,
  `deleted`         tinyint(1)     NOT NULL DEFAULT 0,
  `deleted_at`      datetime(6)    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
