-- Flyway Migration: V002__create_product_tables.sql
-- Creates product-related tables for tenant databases
-- Applies to all tenant databases

-- Create ProductType table
CREATE TABLE IF NOT EXISTS product_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    INDEX idx_code (code),
    INDEX idx_deleted (deleted),
    INDEX idx_updated_at (updated_at),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create AttributeGroup table
CREATE TABLE IF NOT EXISTS attribute_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_type_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    FOREIGN KEY (product_type_id) REFERENCES product_type(id),
    INDEX idx_product_type_id (product_type_id),
    INDEX idx_deleted (deleted),
    INDEX idx_updated_at (updated_at),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create AttributeDefinition table
CREATE TABLE IF NOT EXISTS attribute_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_type_id BIGINT NOT NULL,
    attribute_group_id BIGINT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) NOT NULL COMMENT 'STRING, TEXT, NUMBER, BOOLEAN, DATE',
    required BOOLEAN NOT NULL DEFAULT FALSE,
    searchable BOOLEAN NOT NULL DEFAULT FALSE,
    filterable BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    FOREIGN KEY (product_type_id) REFERENCES product_type(id),
    FOREIGN KEY (attribute_group_id) REFERENCES attribute_group(id),
    UNIQUE KEY unique_code_product_type (code, product_type_id),
    INDEX idx_product_type_id (product_type_id),
    INDEX idx_attribute_group_id (attribute_group_id),
    INDEX idx_deleted (deleted),
    INDEX idx_updated_at (updated_at),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Category table
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    FOREIGN KEY (parent_id) REFERENCES category(id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_deleted (deleted),
    INDEX idx_updated_at (updated_at),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Product table
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_type_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    FOREIGN KEY (product_type_id) REFERENCES product_type(id),
    UNIQUE KEY unique_sku (sku),
    INDEX idx_product_type_id (product_type_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create ProductCategory table
CREATE TABLE IF NOT EXISTS product_category (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, category_id),
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create ProductAttributeValue table
CREATE TABLE IF NOT EXISTS product_attribute_value (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    value_string VARCHAR(1000),
    value_number DECIMAL(15, 4),
    value_boolean BOOLEAN,
    value_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attribute_definition(id),
    UNIQUE KEY unique_product_attribute (product_id, attribute_id),
    INDEX idx_product_id (product_id),
    INDEX idx_attribute_id (attribute_id),
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default product types
INSERT INTO product_type (code, name, description) VALUES
('FOOD', 'Food', 'Food and beverage products'),
('BEVERAGE', 'Beverage', 'Drinks and beverages'),
('DRUG', 'Drug / Pharmacy', 'Pharmaceutical products'),
('CONVENIENCE', 'Convenience Item', 'Convenience store items'),
('BIKE', 'Bike / Motorbike', 'Bicycles and motorcycles'),
('HARDWARE', 'Hardware / Tools', 'Hardware and tools'),
('CLOTHING', 'Clothing / Apparel', 'Clothing and apparel'),
('ELECTRONICS', 'Electronics', 'Electronic devices'),
('FURNITURE', 'Furniture', 'Furniture items'),
('BEAUTY', 'Beauty / Personal Care', 'Beauty and personal care'),
('TOYS', 'Toys / Games', 'Toys and games'),
('BOOKS', 'Books / Media', 'Books and media'),
('SPORTS', 'Sports / Outdoor', 'Sports and outdoor equipment'),
('AUTO_PARTS', 'Automotive Parts', 'Automotive parts'),
('APPLIANCES', 'Home Appliances', 'Home appliances'),
('OFFICE', 'Office Supplies', 'Office supplies'),
('PET', 'Pet Supplies', 'Pet supplies'),
('HEALTH', 'Health / Wellness', 'Health and wellness products')
ON DUPLICATE KEY UPDATE name=VALUES(name);

