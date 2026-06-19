-- ══════════════════════════════════════════════════════════════════════════════
-- FnB service charge (phí dịch vụ) — 4a
-- Per-order percentage applied to the discounted subtotal, stored as its own line on
-- the order (separate from tax). Defaults to 0 so non-FnB orders are unaffected.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE orders ADD COLUMN IF NOT EXISTS service_charge_rate   DECIMAL(5,2)  NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS service_charge_amount DECIMAL(15,2) NOT NULL DEFAULT 0;
