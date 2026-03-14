-- Add soft delete columns to product_attribute_value table
ALTER TABLE product_attribute_value
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false AFTER updated_at,
ADD COLUMN deleted_at DATETIME NULL AFTER deleted;

-- Add index for deleted column for better query performance
ALTER TABLE product_attribute_value
ADD INDEX idx_deleted (deleted);

