-- ══════════════════════════════════════════════════════════════════════════════
-- FnB modifier selections on cart/order lines (4b — slice 2)
-- Stores the chosen modifier options (label + price delta) as JSON on each line so
-- the receipt and kitchen ticket can show them. The numeric delta is already folded
-- into the line's unit price at add-to-cart time.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE cart_items  ADD COLUMN IF NOT EXISTS modifiers JSONB DEFAULT NULL;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS modifiers JSONB DEFAULT NULL;
