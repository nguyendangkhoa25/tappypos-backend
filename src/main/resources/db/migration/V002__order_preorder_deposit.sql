-- ============================================================
-- V002 — Pre-order + deposit workflow (đặt bánh / đặt hàng + tiền cọc)
-- Bakery Phase 2 (docs/BAKERY_PHASE2_PREORDER_DEPOSIT_SPEC.md).
--
-- Additive only: both columns are NOT NULL with safe defaults, so every
-- existing order (all shop types) keeps its current behaviour unchanged
-- (is_preorder = FALSE, deposit_amount = 0). Capability is a MODE of ORDER —
-- no new feature flag, no new status, no new table. Balance due is derived
-- (total_amount - amount_paid), never stored.
-- ============================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS is_preorder    BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deposit_amount DECIMAL(15,2) NOT NULL DEFAULT 0;

-- Pickup queue lookup: PENDING pre-orders ordered by pickup time, scoped per tenant.
CREATE INDEX IF NOT EXISTS idx_orders_preorder_pickup
    ON orders (tenant_id, pickup_time)
    WHERE is_preorder = TRUE AND deleted = FALSE;
