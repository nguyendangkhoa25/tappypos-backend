-- Create inventory table
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    quantity_in_stock BIGINT NOT NULL DEFAULT 0,
    reorder_level BIGINT NOT NULL DEFAULT 10,
    reorder_quantity BIGINT NOT NULL DEFAULT 50,
    unit_cost DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    warehouse_location VARCHAR(255) NOT NULL,
    last_restock_date DATETIME,
    expiry_date DATE,
    batch_number VARCHAR(100),
    notes VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, DISCONTINUED',
    inventory_type VARCHAR(50) NOT NULL DEFAULT 'RETAIL' COMMENT 'RETAIL, WHOLESALE, WAREHOUSE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,

    UNIQUE KEY unique_product_id (product_id),
    INDEX idx_product_id (product_id),
    INDEX idx_warehouse_location (warehouse_location),
    INDEX idx_status (status),
    INDEX idx_inventory_type (inventory_type),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index for low stock items (quantity_in_stock <= reorder_level)
CREATE INDEX idx_low_stock ON inventory(quantity_in_stock, reorder_level, deleted);

-- Create index for expiring items
CREATE INDEX idx_expired_items ON inventory(expiry_date, deleted);

-- Create index for composite search
CREATE INDEX idx_composite_search ON inventory(deleted, status, inventory_type);

