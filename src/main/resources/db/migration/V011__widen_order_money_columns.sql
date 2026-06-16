-- ============================================================
-- V011 — Widen orders money columns DECIMAL(10,2) → DECIMAL(15,2)
-- The 10,2 ceiling (~99,999,999.99) is too low for lodging settlements
-- (a long/high-rate stay can exceed 100M VND), which would overflow and
-- fail checkout. Widening is non-breaking (no precision/scale data loss).
-- ============================================================

ALTER TABLE orders ALTER COLUMN total_amount      TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN amount_paid        TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN change_amount      TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN discount_amount    TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN tax_amount         TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN commission_amount  TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN tip_amount         TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN promotion_discount TYPE DECIMAL(15,2);
ALTER TABLE orders ALTER COLUMN loyalty_discount   TYPE DECIMAL(15,2);
