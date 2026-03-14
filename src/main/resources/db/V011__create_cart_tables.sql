-- Cart/Basket Management Tables
-- Created for POS Feature 1: Cart/Basket Management

-- Create carts table
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID for cart session',
    customer_id BIGINT COMMENT 'Associated customer (optional)',
    subtotal DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_discount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_tax DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|ABANDONED|COMPLETED|PAID',
    applied_coupons LONGTEXT COMMENT 'JSON array of coupon codes',
    applied_promotions LONGTEXT COMMENT 'JSON array of promotion IDs',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    abandoned_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    INDEX idx_cart_id (cart_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Shopping carts for POS system';

-- Create cart_items table
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    base_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    discount_type VARCHAR(50) COMMENT 'NONE|AMOUNT|PERCENTAGE',
    discount_value DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    discount_reason VARCHAR(255),
    line_subtotal DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    tax DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    line_grand_total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    variants JSON COMMENT 'Product variants as JSON object',
    notes TEXT,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    INDEX idx_cart_id (cart_id),
    INDEX idx_product_id (product_id),
    INDEX idx_added_at (added_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Individual line items in shopping carts';

