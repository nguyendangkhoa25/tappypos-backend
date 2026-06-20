-- ══════════════════════════════════════════════════════════════════════════════
-- Idempotency guard for loyalty stamp-card accrual (4b follow-up)
-- Stamp accrual (awardStampsForOrder) had no per-order idempotency, unlike points
-- (which are guarded via loyalty_transactions.order_id). Order completion is one-way
-- in normal flow, but a retry/race could double-award stamps. This flag makes accrual
-- exactly-once per order: the caller sets it after the first accrual and skips if set.
--
-- Additive only: one nullable-with-default column on the existing RLS-protected orders
-- table. FALSE for every existing row and every non-stamp-card tenant → no behaviour change.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE orders ADD COLUMN IF NOT EXISTS stamps_awarded BOOLEAN NOT NULL DEFAULT FALSE;
