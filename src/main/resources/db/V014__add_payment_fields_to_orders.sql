-- V014: Add payment and order tracking columns to orders table
-- These columns are required by the Order entity but were missing from the initial schema.

ALTER TABLE `orders`
    ADD COLUMN `order_number`   VARCHAR(20)    NULL AFTER `id`,
    ADD COLUMN `payment_method` VARCHAR(50)    NULL AFTER `order_number`,
    ADD COLUMN `amount_paid`    DECIMAL(10,2)  NULL AFTER `payment_method`,
    ADD COLUMN `change_amount`  DECIMAL(10,2)  NULL AFTER `amount_paid`,
    MODIFY COLUMN `customer_id` BIGINT         NULL,
    ADD UNIQUE KEY `uq_order_number` (`order_number`);
