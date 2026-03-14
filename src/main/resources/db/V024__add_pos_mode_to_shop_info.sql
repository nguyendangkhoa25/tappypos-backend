-- V024: Add POS mode to shop_info
-- STANDARD = numbered order tabs (retail, spa, etc.)
-- TABLE    = tabs show table names (restaurant, café)

ALTER TABLE shop_info
    ADD COLUMN pos_mode VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
