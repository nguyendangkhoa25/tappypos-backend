-- Order Management System Database Migration Script
-- Run this script to update your database schema

-- 1. Update orders table to change DRAFT status to PENDING
ALTER TABLE orders MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Update any existing DRAFT statuses to PENDING
UPDATE orders SET status = 'PENDING' WHERE status = 'DRAFT';

-- 2. Add new columns to orders table
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS tax_percentage DECIMAL(5,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS commission_amount DECIMAL(10,2) DEFAULT 0;

-- 3. Add new columns to order_items table
ALTER TABLE order_items
ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'READY',
ADD COLUMN IF NOT EXISTS amount_before_tax DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS tax_percentage DECIMAL(5,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS commission_rate DECIMAL(5,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS commission_amount DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS assigned_employee_id BIGINT NULL,
ADD COLUMN IF NOT EXISTS completed_at DATETIME NULL;

-- 4. Add foreign key constraint for assigned_employee_id in order_items
ALTER TABLE order_items
ADD CONSTRAINT IF NOT EXISTS fk_order_items_employee_id
    FOREIGN KEY (assigned_employee_id) REFERENCES employees(id);

-- 5. Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_order_items_status ON order_items(status);
CREATE INDEX IF NOT EXISTS idx_order_items_assigned_employee ON order_items(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_discount ON orders(discount_amount);
CREATE INDEX IF NOT EXISTS idx_orders_tax ON orders(tax_percentage);
CREATE INDEX IF NOT EXISTS idx_orders_commission ON orders(commission_amount);
CREATE INDEX IF NOT EXISTS idx_order_items_commission_rate ON order_items(commission_rate);

-- 6. Verify the schema
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME IN ('orders', 'order_items')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- 7. Check constraints
SELECT CONSTRAINT_NAME, TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME IN ('orders', 'order_items')
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Optional: Rollback commands (if needed)
-- ALTER TABLE order_items DROP FOREIGN KEY fk_order_items_employee_id;
-- ALTER TABLE order_items DROP COLUMN status;
-- ALTER TABLE order_items DROP COLUMN amount_before_tax;
-- ALTER TABLE order_items DROP COLUMN tax_percentage;
-- ALTER TABLE order_items DROP COLUMN tax_amount;
-- ALTER TABLE order_items DROP COLUMN commission_rate;
-- ALTER TABLE order_items DROP COLUMN commission_amount;
-- ALTER TABLE order_items DROP COLUMN assigned_employee_id;
-- ALTER TABLE order_items DROP COLUMN completed_at;
-- ALTER TABLE orders DROP COLUMN discount_amount;
-- ALTER TABLE orders DROP COLUMN tax_percentage;
-- ALTER TABLE orders DROP COLUMN tax_amount;
-- ALTER TABLE orders DROP COLUMN commission_amount;

