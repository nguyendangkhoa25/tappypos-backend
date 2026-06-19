-- ══════════════════════════════════════════════════════════════════════════════
-- Lodging housekeeping workflow (4d)
-- Assign a cleaner to a DIRTY room and track cleaning start/finish times.
-- All columns nullable/additive; existing rooms are unaffected.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE room ADD COLUMN IF NOT EXISTS assigned_cleaner_id   BIGINT       DEFAULT NULL;
ALTER TABLE room ADD COLUMN IF NOT EXISTS assigned_cleaner_name VARCHAR(255) DEFAULT NULL;
ALTER TABLE room ADD COLUMN IF NOT EXISTS cleaning_started_at   TIMESTAMP    DEFAULT NULL;
ALTER TABLE room ADD COLUMN IF NOT EXISTS cleaned_at            TIMESTAMP    DEFAULT NULL;
