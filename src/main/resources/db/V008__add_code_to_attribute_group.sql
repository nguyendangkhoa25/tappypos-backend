-- Flyway Migration: V008__add_code_to_attribute_group.sql
-- Adds code column to attribute_group table and populates with values matching i18n keys
-- Applies to all tenant databases

-- ============================================
-- Add code column to attribute_group table
-- ============================================
ALTER TABLE attribute_group
ADD COLUMN code VARCHAR(100) NOT NULL UNIQUE AFTER product_type_id;

-- ============================================
-- Populate code column based on existing group names
-- Map: "Display Name" → "snake_case_code"
-- ============================================

-- FOOD Product Type Groups
UPDATE attribute_group
SET code = 'nutrition_information'
WHERE name = 'Nutrition Information'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'FOOD');

UPDATE attribute_group
SET code = 'storage_expiry'
WHERE name = 'Storage & Expiry'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'FOOD');

UPDATE attribute_group
SET code = 'origin_certification'
WHERE name = 'Origin & Certification'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'FOOD');

-- DRUG Product Type Groups
UPDATE attribute_group
SET code = 'medical_information'
WHERE name = 'Medical Information'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'DRUG');

UPDATE attribute_group
SET code = 'dosage_form'
WHERE name = 'Dosage & Form'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'DRUG');

UPDATE attribute_group
SET code = 'regulations'
WHERE name = 'Regulations'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'DRUG');

-- ELECTRONICS Product Type Groups
UPDATE attribute_group
SET code = 'technical_specifications'
WHERE name = 'Technical Specifications'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'ELECTRONICS');

UPDATE attribute_group
SET code = 'warranty_support'
WHERE name = 'Warranty & Support'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'ELECTRONICS');

-- CLOTHING Product Type Groups
UPDATE attribute_group
SET code = 'sizing_fit'
WHERE name = 'Sizing & Fit'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'CLOTHING');

UPDATE attribute_group
SET code = 'material_care'
WHERE name = 'Material & Care'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'CLOTHING');

-- BIKE Product Type Groups
UPDATE attribute_group
SET code = 'engine_specifications'
WHERE name = 'Engine Specifications'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'BIKE');

UPDATE attribute_group
SET code = 'performance'
WHERE name = 'Performance'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'BIKE');

UPDATE attribute_group
SET code = 'maintenance'
WHERE name = 'Maintenance'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'BIKE');

-- BEVERAGE Product Type Groups
UPDATE attribute_group
SET code = 'nutrition_info'
WHERE name = 'Nutrition Info'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'BEVERAGE');

UPDATE attribute_group
SET code = 'storage_conditions'
WHERE name = 'Storage Conditions'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'BEVERAGE');

-- CONVENIENCE Product Type Groups
UPDATE attribute_group
SET code = 'basic_information'
WHERE name = 'Basic Information'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'CONVENIENCE');

UPDATE attribute_group
SET code = 'storage_information'
WHERE name = 'Storage Information'
AND product_type_id = (SELECT id FROM product_type WHERE code = 'CONVENIENCE');

-- ============================================
-- For any remaining groups without codes (fallback)
-- Auto-generate code from name (convert to snake_case)
-- ============================================
UPDATE attribute_group
SET code = LOWER(REPLACE(REPLACE(REPLACE(name, ' & ', '_'), ' ', '_'), '-', '_'))
WHERE code IS NULL OR code = '';

-- ============================================
-- Create index on code column for fast lookups
-- ============================================
CREATE INDEX idx_attribute_group_code ON attribute_group(code);
CREATE INDEX idx_attribute_group_product_type_code ON attribute_group(product_type_id, code);

