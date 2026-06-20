-- ══════════════════════════════════════════════════════════════════════════════
-- Loyalty stamp card — "mua N ly tặng 1" (4b)
-- An OPT-IN punch/stamp-card mode layered on the existing per-tenant loyalty program.
-- Café-distinctive but built generically. Disabled by default (stamp_card_enabled =
-- FALSE) so every existing tenant and all other shop types are completely unchanged —
-- the points program keeps working exactly as before.
--
-- Additive only: new columns on the existing loyalty_programs + customers tables
-- (both already RLS-protected). No new tables, so no new RLS policy is required.
-- ══════════════════════════════════════════════════════════════════════════════

-- Program-level config (one row per tenant).
ALTER TABLE loyalty_programs
    ADD COLUMN IF NOT EXISTS stamp_card_enabled  BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE loyalty_programs
    ADD COLUMN IF NOT EXISTS stamp_card_size     INT          NOT NULL DEFAULT 10;
ALTER TABLE loyalty_programs
    ADD COLUMN IF NOT EXISTS stamp_card_reward   VARCHAR(255) NOT NULL DEFAULT 'Tặng 1 ly nước bất kỳ';

-- Per-customer progress: current stamps on the active card + filled cards not yet used.
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS stamp_count    INT NOT NULL DEFAULT 0;
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS stamp_rewards  INT NOT NULL DEFAULT 0;
