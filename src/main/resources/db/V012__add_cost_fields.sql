-- V012: Add cost tracking fields for profit/revenue calculations
-- Adds cost_price to product, unit_cost+cost_amount to order_items,
-- and unit_cost to cart_items so gross profit can be calculated at any level.

-- 1. Product: standard/default cost price (covers services with no inventory)
ALTER TABLE product
    ADD COLUMN cost_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Standard cost/purchase price. For physical products reflects inventory unit_cost; for services set manually.';

-- 2. Order items: snapshot the cost at time of sale (immutable after order placed)
ALTER TABLE order_items
    ADD COLUMN unit_cost  DECIMAL(15, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Cost per unit at time of sale — snapshot from inventory.unit_cost or product.cost_price',
    ADD COLUMN cost_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Total cost for this line (unit_cost × quantity)';

-- 3. Cart items: capture cost when product is added to cart (used during checkout)
ALTER TABLE cart_items
    ADD COLUMN unit_cost DECIMAL(19, 2) NOT NULL DEFAULT 0.00
        COMMENT 'Cost per unit captured at time of cart addition (from inventory or product cost_price)';
