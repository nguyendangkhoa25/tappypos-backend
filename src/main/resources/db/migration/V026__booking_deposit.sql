-- ══════════════════════════════════════════════════════════════════════════════
-- Booking deposit (đặt cọc giữ sân) — SPORT_COURT / BILLIARDS_HALL rental verticals.
-- A regular/casual reserves a court ahead and pays a deposit to hold the slot; the
-- deposit is netted against the bill at checkout. Stored as two columns on bookings
-- (deposit_amount + deposit_paid) — bookings already has RLS + legacy_id (V001).
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS deposit_amount DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS deposit_paid    BOOLEAN       NOT NULL DEFAULT FALSE;
