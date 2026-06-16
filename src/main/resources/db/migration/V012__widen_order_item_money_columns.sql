-- ============================================================
-- V012 — Widen order_items money columns DECIMAL(10,2) → DECIMAL(15,2)
-- Companion to V011: a lodging settlement's room-charge line item carries the
-- full stay total, which can exceed the old 100M VND ceiling and overflow on
-- the order_items row. Widening is non-breaking.
-- ============================================================

ALTER TABLE order_items ALTER COLUMN unit_price        TYPE DECIMAL(15,2);
ALTER TABLE order_items ALTER COLUMN amount            TYPE DECIMAL(15,2);
ALTER TABLE order_items ALTER COLUMN tax_amount        TYPE DECIMAL(15,2);
ALTER TABLE order_items ALTER COLUMN commission_amount TYPE DECIMAL(15,2);
ALTER TABLE order_items ALTER COLUMN amount_before_tax TYPE DECIMAL(15,2);
