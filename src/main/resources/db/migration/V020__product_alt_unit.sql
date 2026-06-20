-- ══════════════════════════════════════════════════════════════════════════════
-- Product alternate sell unit / unit conversion (bán lẻ vs bán sỉ)
-- A product is stocked in its base `unit`; it may also be sold in ONE alternate unit
-- (e.g. base = "kg", alt = "bao" with factor 50 → 1 bao = 50 kg). When sold in the
-- alt unit, stock is deducted in the base unit (qty × factor) and priced per alt unit.
--
-- Additive, nullable columns on the existing RLS-protected product table — no new
-- table, so no new RLS policy. Existing products (alt_unit NULL) are unaffected.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE product ADD COLUMN IF NOT EXISTS alt_unit        VARCHAR(20)   DEFAULT NULL;
ALTER TABLE product ADD COLUMN IF NOT EXISTS alt_unit_factor DECIMAL(15,3) DEFAULT NULL;
ALTER TABLE product ADD COLUMN IF NOT EXISTS alt_unit_price  DECIMAL(15,2) DEFAULT NULL;
