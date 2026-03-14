-- V013: Add structured shelf/bin location fields to inventory
-- Supports finding products by zone (kho), aisle (hàng), shelf (kệ), bin (ô)

ALTER TABLE inventory
    ADD COLUMN zone    VARCHAR(50) NULL COMMENT 'Khu vực / Kho (e.g. A, MAIN, COLD)',
    ADD COLUMN aisle   VARCHAR(20) NULL COMMENT 'Hàng (e.g. 1, 2, 3)',
    ADD COLUMN shelf   VARCHAR(20) NULL COMMENT 'Kệ (e.g. A, B, C)',
    ADD COLUMN bin     VARCHAR(20) NULL COMMENT 'Ô / Ngăn (e.g. 01, 02)';
