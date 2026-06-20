-- ══════════════════════════════════════════════════════════════════════════════
-- Recurring fixed weekly slots (sân cố định) — SPORT_COURT regulars book the same
-- slot every week (e.g. every Tuesday 18:00–20:00). We MATERIALIZE future bookings:
-- creating a recurring reservation inserts N concrete weekly rows that all share a
-- recurrence_group_id, so the calendar grid and overlap checks treat them as ordinary
-- bookings. The group id links them for "edit/cancel the whole series" operations.
-- bookings already has RLS + legacy_id (V001).
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS recurrence_group_id VARCHAR(36) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_bookings_recurrence_group
    ON bookings (tenant_id, recurrence_group_id) WHERE recurrence_group_id IS NOT NULL;
