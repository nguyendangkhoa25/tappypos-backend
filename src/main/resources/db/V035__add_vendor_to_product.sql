-- Add vendor FK to product table for direct supplier relationship
ALTER TABLE product
    ADD COLUMN vendor_id BIGINT NULL COMMENT 'Supplier/vendor for this product',
    ADD CONSTRAINT fk_product_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id);
