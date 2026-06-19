-- ══════════════════════════════════════════════════════════════════════════════
-- FnB order-type channel (dine-in / takeaway / delivery) — 4d
-- Explicit fulfilment channel, separate from order_type (which is gold-trading only).
-- Defaults to DINE_IN; back-filled from existing signals below.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_channel VARCHAR(20) NOT NULL DEFAULT 'DINE_IN';

-- Back-fill: orders with a pickup_time (and no table) were takeaway.
UPDATE orders SET order_channel = 'TAKEAWAY'
WHERE pickup_time IS NOT NULL AND table_id IS NULL AND order_channel = 'DINE_IN';

CREATE INDEX IF NOT EXISTS idx_orders_channel
    ON orders (tenant_id, order_channel) WHERE deleted = FALSE;
