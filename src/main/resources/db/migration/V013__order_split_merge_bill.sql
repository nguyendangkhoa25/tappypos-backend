-- ══════════════════════════════════════════════════════════════════════════════
-- Split / merge bill (tách / gộp bill) — FnB §4b (restaurant headline gap)
-- A split produces N child checks from one running tab; a merge folds one table's
-- tab into another. Child checks point back at the source via parent_order_id so a
-- table's split group can be tracked and the table released only once every check
-- in the group is settled. Additive + nullable — non-FnB orders are unaffected
-- (parent_order_id stays NULL for every normal order).
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE orders ADD COLUMN IF NOT EXISTS parent_order_id BIGINT DEFAULT NULL;

-- Reverse-lookup of a split group's child checks (only the few split rows are indexed).
CREATE INDEX IF NOT EXISTS idx_orders_parent_order_id
    ON orders (tenant_id, parent_order_id) WHERE parent_order_id IS NOT NULL;
