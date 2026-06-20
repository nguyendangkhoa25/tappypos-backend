-- ══════════════════════════════════════════════════════════════════════════════
-- Wholesale vs retail price tiers (giá sỉ vs giá lẻ), customer-type driven.
-- A product may carry a wholesale_price (per base unit); a customer flagged
-- customer_type = WHOLESALE (e.g. a contractor) is charged that price at POS.
--
-- Additive nullable columns on existing RLS-protected tables — no new table/policy.
-- Existing rows: wholesale_price NULL (no tier), customer_type 'RETAIL' (default).
-- Distinct from V020/V021 alt-unit pricing (which is per-unit, not per-customer).
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE product   ADD COLUMN IF NOT EXISTS wholesale_price DECIMAL(15,2) DEFAULT NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS customer_type   VARCHAR(20)   NOT NULL DEFAULT 'RETAIL'; -- RETAIL | WHOLESALE
