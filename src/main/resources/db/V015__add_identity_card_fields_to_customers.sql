-- V015: Add identity card fields to customers table

ALTER TABLE `customers`
    ADD COLUMN `id_card_number`    VARCHAR(20)   NULL UNIQUE     AFTER `special_requests`,
    ADD COLUMN `date_of_birth`     DATE          NULL            AFTER `id_card_number`,
    ADD COLUMN `gender`            VARCHAR(10)   NULL            AFTER `date_of_birth`,
    ADD COLUMN `id_card_issued_date` DATE        NULL            AFTER `gender`,
    ADD COLUMN `id_card_issued_place` VARCHAR(255) NULL          AFTER `id_card_issued_date`,
    ADD COLUMN `permanent_address` VARCHAR(500)  NULL            AFTER `id_card_issued_place`;
