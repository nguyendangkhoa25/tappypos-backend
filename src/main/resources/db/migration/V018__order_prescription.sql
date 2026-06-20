-- ══════════════════════════════════════════════════════════════════════════════
-- Prescription dispensing at POS (Pharmacy — PHARMACY_SHOP_TYPE_PLAN.md §4d)
-- Pharmacy seeds a DRUG `prescription_required` attribute (V… pharmacy.sql). This
-- lets the POS flag prescription drugs in the cart and record who prescribed +
-- a dispensing note on the order — a non-blocking paper trail for a nhà thuốc.
--
-- Additive only: new nullable / default-FALSE columns on existing RLS-protected
-- tenant tables. They are populated only when a prescription drug is sold, so
-- every non-pharmacy order and every other shop type is completely unchanged.
-- ══════════════════════════════════════════════════════════════════════════════

-- Whether the line item is a prescription-required drug (copied from the product's
-- `prescription_required` EAV attribute at add-to-cart / checkout time).
ALTER TABLE cart_items  ADD COLUMN IF NOT EXISTS prescription_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS prescription_required BOOLEAN NOT NULL DEFAULT FALSE;

-- Prescriber + dispensing note recorded once per order at checkout (optional).
ALTER TABLE orders ADD COLUMN IF NOT EXISTS prescriber_name   VARCHAR(255)  DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS prescription_note VARCHAR(1000) DEFAULT NULL;
