-- ══════════════════════════════════════════════════════════════════════════════
-- Order delivery details (GrabFood / ShopeeFood / self-delivery) — 4e
-- The DELIVERY order channel and per-channel revenue reporting already exist
-- (V007 + ChannelReportService). This adds the delivery fulfilment detail that a
-- café needs: who/where to deliver, which platform, the ship fee, and a delivery
-- status the staff can advance (PENDING → DELIVERING → DELIVERED).
--
-- Additive only: new nullable columns on the existing (RLS-protected) orders table.
-- They are populated only when order_channel = 'DELIVERY', so every non-delivery
-- order and every other shop type is completely unchanged.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_platform  VARCHAR(30)   DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_recipient VARCHAR(150)  DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_phone     VARCHAR(50)   DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address   VARCHAR(500)  DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee       DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_note      VARCHAR(500)  DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_status    VARCHAR(20)   DEFAULT NULL;

-- Lookups for the "đơn giao hàng đang chờ" list (delivery orders not yet delivered).
CREATE INDEX IF NOT EXISTS idx_orders_delivery_status
    ON orders (tenant_id, delivery_status)
    WHERE delivery_status IS NOT NULL AND deleted = FALSE;
