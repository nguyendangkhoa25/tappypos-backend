-- ============================================================
-- V009 — Room reservations (advance bookings)
-- A stay can now be created in RESERVED status before the guest arrives:
--   reserved_checkin / expected_checkout hold the planned dates; checkin_at
--   stays NULL until the guest actually checks in (RESERVED → IN_HOUSE).
-- New room_stay.status values: RESERVED, NO_SHOW (string column — no enum change).
-- ============================================================

ALTER TABLE room_stay ALTER COLUMN checkin_at DROP NOT NULL;
ALTER TABLE room_stay ADD COLUMN IF NOT EXISTS reserved_checkin TIMESTAMP DEFAULT NULL;

-- Calendar / upcoming-reservations lookups by planned arrival.
CREATE INDEX IF NOT EXISTS idx_room_stay_reserved ON room_stay (tenant_id, status, reserved_checkin);
