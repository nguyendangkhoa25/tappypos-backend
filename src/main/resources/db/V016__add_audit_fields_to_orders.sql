-- V016: Add audit fields to orders table
-- Tracks who created/completed/cancelled/voided each order, plus void columns.

ALTER TABLE `orders`
    ADD COLUMN `created_by`   VARCHAR(100) NULL AFTER `notes`,
    ADD COLUMN `completed_by` VARCHAR(100) NULL AFTER `completed_at`,
    ADD COLUMN `cancelled_at` DATETIME     NULL AFTER `completed_by`,
    ADD COLUMN `cancel_reason` VARCHAR(500) NULL AFTER `cancelled_at`,
    ADD COLUMN `cancelled_by` VARCHAR(100) NULL AFTER `cancel_reason`,
    ADD COLUMN `voided_at`    DATETIME     NULL AFTER `cancelled_by`,
    ADD COLUMN `void_reason`  VARCHAR(500) NULL AFTER `voided_at`,
    ADD COLUMN `voided_by`    VARCHAR(100) NULL AFTER `void_reason`;
