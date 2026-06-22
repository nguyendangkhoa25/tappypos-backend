-- Activity Log i18n: store a message key + JSON args instead of a frozen rendered string.
-- The description is now rendered at read time in the reader's locale (see ActivityLogServiceImpl).
-- Legacy rows keep their existing `description` and render through the fallback path
-- (description_key IS NULL -> show description verbatim). No backfill is performed.

ALTER TABLE activity_log ADD COLUMN IF NOT EXISTS description_key  VARCHAR(150) DEFAULT NULL;
ALTER TABLE activity_log ADD COLUMN IF NOT EXISTS description_args TEXT         DEFAULT NULL;

-- New rows no longer populate `description` (key + args are the source of truth),
-- so it must be nullable.
ALTER TABLE activity_log ALTER COLUMN description DROP NOT NULL;
