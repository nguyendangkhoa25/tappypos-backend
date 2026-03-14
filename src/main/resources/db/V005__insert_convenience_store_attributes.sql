-- ============================================================================
-- V005__insert_convenience_store_attributes.sql
-- ============================================================================
-- Insert attributes for Convenience Store Product Type
--
-- This migration adds all attribute definitions and groups for the Convenience Store
-- product type, which supports general retail items like groceries, beverages,
-- snacks, toiletries, and household items.
-- ============================================================================

-- Step 1: Get the product_type_id for CONVENIENCE
-- (This assumes CONVENIENCE product type already exists from V004)

-- Step 2: Create Attribute Groups for Convenience Store
INSERT INTO attribute_group (product_type_id, name, display_order)
SELECT id, 'Basic Information', 1 FROM product_type WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 1;

INSERT INTO attribute_group (product_type_id, name, display_order)
SELECT id, 'Storage & Handling', 2 FROM product_type WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 2;

INSERT INTO attribute_group (product_type_id, name, display_order)
SELECT id, 'Supplier Information', 3 FROM product_type WHERE code = 'CONVENIENCE'
ON DUPLICATE KEY UPDATE display_order = 3;

-- Step 3: Insert Attribute Definitions for Convenience Store

-- ============================================================================
-- GROUP 1: BASIC INFORMATION
-- ============================================================================

-- Brand/Manufacturer
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'brand',
    'Brand / Manufacturer',
    'STRING',
    FALSE,
    TRUE,
    TRUE,
    1
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Basic Information'
ON DUPLICATE KEY UPDATE name = 'Brand / Manufacturer';

-- Category (Beverages, Snacks, Personal Care, Household, etc.)
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'item_category',
    'Category',
    'STRING',
    TRUE,
    TRUE,
    TRUE,
    2
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Basic Information'
ON DUPLICATE KEY UPDATE name = 'Category';

-- Package Size (e.g., 330ml, 500g, 1kg)
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'package_size',
    'Package Size',
    'STRING',
    FALSE,
    TRUE,
    TRUE,
    3
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Basic Information'
ON DUPLICATE KEY UPDATE name = 'Package Size';

-- Country of Origin
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'country_of_origin',
    'Country of Origin',
    'STRING',
    FALSE,
    TRUE,
    TRUE,
    4
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Basic Information'
ON DUPLICATE KEY UPDATE name = 'Country of Origin';

-- ============================================================================
-- GROUP 2: STORAGE & HANDLING
-- ============================================================================

-- Expiry Date (for perishables)
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'expiry_date',
    'Expiry Date',
    'DATE',
    FALSE,
    FALSE,
    TRUE,
    1
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Storage & Handling'
ON DUPLICATE KEY UPDATE name = 'Expiry Date';

-- Storage Requirements (Room Temperature, Refrigerated, Frozen)
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'storage_requirement',
    'Storage Requirement',
    'STRING',
    FALSE,
    FALSE,
    TRUE,
    2
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Storage & Handling'
ON DUPLICATE KEY UPDATE name = 'Storage Requirement';

-- Temperature Range (if applicable)
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'temperature_range',
    'Storage Temperature Range (°C)',
    'STRING',
    FALSE,
    FALSE,
    FALSE,
    3
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Storage & Handling'
ON DUPLICATE KEY UPDATE name = 'Storage Temperature Range (°C)';

-- Handling Instructions
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'handling_instructions',
    'Handling Instructions',
    'TEXT',
    FALSE,
    FALSE,
    FALSE,
    4
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Storage & Handling'
ON DUPLICATE KEY UPDATE name = 'Handling Instructions';

-- ============================================================================
-- GROUP 3: SUPPLIER INFORMATION
-- ============================================================================

-- Supplier Name
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'supplier_name',
    'Supplier Name',
    'STRING',
    FALSE,
    TRUE,
    TRUE,
    1
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Supplier Information'
ON DUPLICATE KEY UPDATE name = 'Supplier Name';

-- Supplier SKU/Code
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'supplier_sku',
    'Supplier SKU / Code',
    'STRING',
    FALSE,
    TRUE,
    FALSE,
    2
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Supplier Information'
ON DUPLICATE KEY UPDATE name = 'Supplier SKU / Code';

-- Barcode / UPC
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT
    pt.id,
    ag.id,
    'barcode_upc',
    'Barcode / UPC',
    'STRING',
    FALSE,
    TRUE,
    FALSE,
    3
FROM product_type pt, attribute_group ag
WHERE pt.code = 'CONVENIENCE' AND ag.product_type_id = pt.id AND ag.name = 'Supplier Information'
ON DUPLICATE KEY UPDATE name = 'Barcode / UPC';

-- ============================================================================
-- Notes for Implementation
-- ============================================================================
--
-- Data Types Available:
--   - STRING: Short text (up to 1000 chars) - brand, category, package size
--   - TEXT: Long text (up to 1000 chars) - handling instructions, notes
--   - NUMBER: Decimal numbers (15,4) - weight, volume, cost
--   - BOOLEAN: True/False values
--   - DATE: Date values - expiry date, manufacture date
--
-- Flags:
--   - required: User MUST fill this field when creating a product
--   - searchable: Field is included in search/filter operations
--   - filterable: Field can be used in advanced filters
--
-- Display Order: Controls the order attributes appear in the UI form
--
-- ============================================================================
-- Optional: Create categories for convenience store
-- (If you have a separate categories table)
-- ============================================================================

-- If you want predefined categories, uncomment and adjust:
/*
INSERT INTO category (name, parent_id) VALUES
('Beverages', NULL),
('Snacks', NULL),
('Personal Care', NULL),
('Household Items', NULL),
('Dairy & Eggs', NULL),
('Frozen Foods', NULL),
('Bakery', NULL),
('Candies & Sweets', NULL)
ON DUPLICATE KEY UPDATE name = name;
*/

-- ============================================================================
-- Verification Query
-- ============================================================================
-- Run this to verify attributes were inserted:
--
-- SELECT
--   ag.name as 'Attribute Group',
--   ad.code as 'Code',
--   ad.name as 'Attribute Name',
--   ad.data_type as 'Type',
--   ad.required as 'Required',
--   ad.display_order as 'Order'
-- FROM attribute_definition ad
-- JOIN attribute_group ag ON ad.attribute_group_id = ag.id
-- JOIN product_type pt ON ad.product_type_id = pt.id
-- WHERE pt.code = 'CONVENIENCE'
-- ORDER BY ag.display_order, ad.display_order;
--
-- ============================================================================

