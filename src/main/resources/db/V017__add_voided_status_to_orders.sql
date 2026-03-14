-- V017: Add VOIDED to the orders.status enum
ALTER TABLE `orders`
    MODIFY COLUMN `status` ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED')
        COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING';
