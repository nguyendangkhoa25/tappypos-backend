-- ══════════════════════════════════════════════════════════════════════════════
-- Quotations / báo giá. A quote is an order with is_quote = TRUE: it holds line items
-- and totals like a normal order but does NOT deduct stock until it is converted to a
-- real order. Gated by the existing ORDER feature (no new flag).
--
-- Additive nullable/defaulted columns on the existing RLS-protected orders table.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE orders ADD COLUMN IF NOT EXISTS is_quote     BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS quote_number VARCHAR(50) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_is_quote
    ON orders (tenant_id, is_quote) WHERE deleted = FALSE AND is_quote = TRUE;
