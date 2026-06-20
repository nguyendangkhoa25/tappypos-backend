-- ══════════════════════════════════════════════════════════════════════════════
-- Sell-in-alternate-unit at POS (bán lẻ vs bán sỉ) — line-item carry of the chosen
-- unit + conversion factor. Pairs with V020 (product.alt_unit/alt_unit_factor).
--
-- A line sold in the alternate unit stores quantity in that sell unit (e.g. 2 "bao"),
-- plus the factor (base units per sell unit, e.g. 50). Stock is deducted in the base
-- unit: round(quantity × unit_factor). unit_factor NULL/1 = normal single-unit line,
-- so all existing rows and shop types are unaffected.
--
-- Additive nullable columns on existing RLS-protected tables — no new table/policy.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS sell_unit   VARCHAR(20)   DEFAULT NULL;
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS unit_factor DECIMAL(15,3) DEFAULT NULL;

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS sell_unit   VARCHAR(20)   DEFAULT NULL;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS unit_factor DECIMAL(15,3) DEFAULT NULL;
