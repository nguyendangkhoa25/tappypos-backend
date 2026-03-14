-- Flyway Migration: V007__seed_product_attributes.sql
-- Seeds initial attributes for default product types
-- Applies to all tenant databases

-- ============================================
-- FOOD Product Type Attributes
-- ============================================
-- Get FOOD product type ID
SET @food_type_id = (SELECT id FROM product_type WHERE code = 'FOOD' LIMIT 1);

-- Create Attribute Groups for FOOD
INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @food_type_id, 'nutrition_information', 'Nutrition Information', 1
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @food_type_id AND name = 'Nutrition Information'
);

INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @food_type_id, 'storage_expiry', 'Storage & Expiry', 2
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @food_type_id AND name = 'Storage & Expiry'
);

INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @food_type_id, 'origin_certification', 'Origin & Certification', 3
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @food_type_id AND name = 'Origin & Certification'
);

-- Insert attributes for FOOD - Nutrition Information
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Nutrition Information' LIMIT 1),
        'calories',
        'Calories per serving (kcal)',
        'NUMBER',
        1, 1, 1, 1
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'calories'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Nutrition Information' LIMIT 1),
        'protein_grams',
        'Protein (grams)',
        'NUMBER',
        0, 1, 0, 2
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'protein_grams'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Nutrition Information' LIMIT 1),
        'fat_grams',
        'Fat (grams)',
        'NUMBER',
        0, 1, 0, 3
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'fat_grams'
);

-- Insert attributes for FOOD - Storage & Expiry
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Storage & Expiry' LIMIT 1),
        'expiry_date',
        'Expiry Date',
        'DATE',
        1, 0, 0, 1
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'expiry_date'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Storage & Expiry' LIMIT 1),
        'storage_temperature',
        'Storage Temperature (°C)',
        'STRING',
        0, 0, 1, 2
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'storage_temperature'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Storage & Expiry' LIMIT 1),
        'ingredients',
        'Ingredients',
        'TEXT',
        1, 1, 0, 3
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'ingredients'
);

-- Insert attributes for FOOD - Origin & Certification
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Origin & Certification' LIMIT 1),
        'country_of_origin',
        'Country of Origin',
        'STRING',
        0, 1, 1, 1
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'country_of_origin'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @food_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @food_type_id AND name = 'Origin & Certification' LIMIT 1),
        'organic_certified',
        'Organic Certified',
        'BOOLEAN',
        0, 0, 1, 2
WHERE @food_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @food_type_id AND code = 'organic_certified'
);

-- ============================================
-- DRUG Product Type Attributes
-- ============================================
SET @drug_type_id = (SELECT id FROM product_type WHERE code = 'DRUG' LIMIT 1);

-- Create Attribute Groups for DRUG
INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @drug_type_id, 'medical_information', 'Medical Information', 1
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @drug_type_id AND name = 'Medical Information'
);

INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @drug_type_id, 'dosage_form', 'Dosage & Form', 2
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @drug_type_id AND name = 'Dosage & Form'
);

INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @drug_type_id, 'regulations', 'Regulations', 3
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @drug_type_id AND name = 'Regulations'
);

-- Insert attributes for DRUG - Medical Information
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Medical Information' LIMIT 1),
        'active_ingredient',
        'Active Ingredient',
        'STRING',
        1, 1, 1, 1
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'active_ingredient'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Medical Information' LIMIT 1),
        'therapeutic_use',
        'Therapeutic Use',
        'TEXT',
        1, 1, 0, 2
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'therapeutic_use'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Medical Information' LIMIT 1),
        'side_effects',
        'Known Side Effects',
        'TEXT',
        0, 0, 0, 3
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'side_effects'
);

-- Insert attributes for DRUG - Dosage & Form
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Dosage & Form' LIMIT 1),
        'dosage_strength',
        'Dosage Strength (mg)',
        'STRING',
        1, 1, 1, 1
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'dosage_strength'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Dosage & Form' LIMIT 1),
        'form',
        'Drug Form',
        'STRING',
        1, 1, 1, 2
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'form'
);

-- Insert attributes for DRUG - Regulations
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Regulations' LIMIT 1),
        'prescription_required',
        'Prescription Required',
        'BOOLEAN',
        1, 0, 1, 1
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'prescription_required'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @drug_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @drug_type_id AND name = 'Regulations' LIMIT 1),
        'manufacturer',
        'Manufacturer Name',
        'STRING',
        1, 1, 0, 2
WHERE @drug_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @drug_type_id AND code = 'manufacturer'
);

-- ============================================
-- ELECTRONICS Product Type Attributes
-- ============================================
SET @electronics_type_id = (SELECT id FROM product_type WHERE code = 'ELECTRONICS' LIMIT 1);

-- Create Attribute Groups for ELECTRONICS
INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @electronics_type_id, 'technical_specifications', 'Technical Specifications', 1
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @electronics_type_id AND name = 'Technical Specifications'
);

INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @electronics_type_id, 'warranty_support', 'Warranty & Support', 2
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @electronics_type_id AND name = 'Warranty & Support'
);

-- Insert attributes for ELECTRONICS - Technical Specifications
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Technical Specifications' LIMIT 1),
        'processor',
        'Processor',
        'STRING',
        1, 1, 1, 1
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'processor'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Technical Specifications' LIMIT 1),
        'ram_gb',
        'RAM (GB)',
        'NUMBER',
        1, 1, 1, 2
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'ram_gb'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Technical Specifications' LIMIT 1),
        'storage_gb',
        'Storage (GB)',
        'NUMBER',
        1, 1, 1, 3
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'storage_gb'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Technical Specifications' LIMIT 1),
        'display_size_inch',
        'Display Size (inches)',
        'NUMBER',
        0, 1, 1, 4
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'display_size_inch'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Technical Specifications' LIMIT 1),
        'battery_capacity_mah',
        'Battery Capacity (mAh)',
        'NUMBER',
        0, 1, 0, 5
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'battery_capacity_mah'
);

-- Insert attributes for ELECTRONICS - Warranty & Support
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Warranty & Support' LIMIT 1),
        'warranty_months',
        'Warranty (months)',
        'NUMBER',
        1, 0, 1, 1
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'warranty_months'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @electronics_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @electronics_type_id AND name = 'Warranty & Support' LIMIT 1),
        'supported_os',
        'Supported OS',
        'TEXT',
        0, 1, 0, 2
WHERE @electronics_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @electronics_type_id AND code = 'supported_os'
);

-- ============================================
-- CLOTHING Product Type Attributes
-- ============================================
SET @clothing_type_id = (SELECT id FROM product_type WHERE code = 'CLOTHING' LIMIT 1);

-- Create Attribute Groups for CLOTHING
INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @clothing_type_id, 'sizing_fit', 'Sizing & Fit', 1
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @clothing_type_id AND name = 'Sizing & Fit'
);

INSERT INTO attribute_group (product_type_id, code, name, display_order)
SELECT @clothing_type_id, 'material_care', 'Material & Care', 2
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_group
    WHERE product_type_id = @clothing_type_id AND name = 'Material & Care'
);

-- Insert attributes for CLOTHING - Sizing & Fit
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @clothing_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @clothing_type_id AND name = 'Sizing & Fit' LIMIT 1),
        'size',
        'Size',
        'STRING',
        1, 1, 1, 1
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @clothing_type_id AND code = 'size'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @clothing_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @clothing_type_id AND name = 'Sizing & Fit' LIMIT 1),
        'color',
        'Color',
        'STRING',
        1, 1, 1, 2
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @clothing_type_id AND code = 'color'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @clothing_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @clothing_type_id AND name = 'Sizing & Fit' LIMIT 1),
        'fit_type',
        'Fit Type',
        'STRING',
        0, 1, 1, 3
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @clothing_type_id AND code = 'fit_type'
);

-- Insert attributes for CLOTHING - Material & Care
INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @clothing_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @clothing_type_id AND name = 'Material & Care' LIMIT 1),
        'material_composition',
        'Material Composition',
        'STRING',
        1, 1, 1, 1
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @clothing_type_id AND code = 'material_composition'
);

INSERT INTO attribute_definition (product_type_id, attribute_group_id, code, name, data_type, required, searchable, filterable, display_order)
SELECT @clothing_type_id,
        (SELECT id FROM attribute_group WHERE product_type_id = @clothing_type_id AND name = 'Material & Care' LIMIT 1),
        'care_instructions',
        'Care Instructions',
        'TEXT',
        0, 0, 0, 2
WHERE @clothing_type_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM attribute_definition
    WHERE product_type_id = @clothing_type_id AND code = 'care_instructions'
);

