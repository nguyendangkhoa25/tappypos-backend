-- Flyway Migration: V006__add_missing_columns_to_product_type.sql
-- Adds missing columns to product_type, attribute_group, attribute_definition, and category tables to match BaseEntity fields
-- Applies to all tenant databases

-- ============================================
-- Add missing columns to product_type table
-- ============================================
ALTER TABLE product_type
ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE product_type
ADD COLUMN deleted_at DATETIME DEFAULT NULL AFTER deleted;

-- Create indexes for the new columns
ALTER TABLE product_type
ADD INDEX idx_updated_at (updated_at);

ALTER TABLE product_type
ADD INDEX idx_deleted_at (deleted_at);

-- Update existing product_type records to have updated_at set to created_at if needed
-- Note: The DEFAULT CURRENT_TIMESTAMP already set updated_at, so we only update if it's very old
UPDATE product_type
SET updated_at = created_at
WHERE updated_at < '1970-01-02';

-- ============================================
-- Add missing columns to attribute_group table
-- ============================================
ALTER TABLE attribute_group
ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE attribute_group
ADD COLUMN deleted_at DATETIME DEFAULT NULL AFTER deleted;

-- Create indexes for the new columns
ALTER TABLE attribute_group
ADD INDEX idx_updated_at (updated_at);

ALTER TABLE attribute_group
ADD INDEX idx_deleted_at (deleted_at);

-- Update existing attribute_group records to have updated_at set to created_at if needed
UPDATE attribute_group
SET updated_at = created_at
WHERE updated_at < '1970-01-02';

-- ============================================
-- Add missing columns to attribute_definition table
-- ============================================
ALTER TABLE attribute_definition
ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE attribute_definition
ADD COLUMN deleted_at DATETIME DEFAULT NULL AFTER deleted;

-- Create indexes for the new columns
ALTER TABLE attribute_definition
ADD INDEX idx_updated_at (updated_at);

ALTER TABLE attribute_definition
ADD INDEX idx_deleted_at (deleted_at);

-- Update existing attribute_definition records to have updated_at set to created_at if needed
UPDATE attribute_definition
SET updated_at = created_at
WHERE updated_at < '1970-01-02';

-- ============================================
-- Add missing columns to category table
-- ============================================
ALTER TABLE category
ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE category
ADD COLUMN deleted_at DATETIME DEFAULT NULL AFTER deleted;

-- Create indexes for the new columns
ALTER TABLE category
ADD INDEX idx_updated_at (updated_at);

ALTER TABLE category
ADD INDEX idx_deleted_at (deleted_at);

-- Update existing category records to have updated_at set to created_at if needed
UPDATE category
SET updated_at = created_at
WHERE updated_at < '1970-01-02';


