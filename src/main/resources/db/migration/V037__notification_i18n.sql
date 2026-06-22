-- Notification i18n: store title/message as message keys + JSON args instead of frozen rendered
-- strings, so each notification renders in the reader's locale at read time
-- (see NotificationService.mapToDTO). System-generated notifications populate the *_key/*_args
-- columns; user-authored ones (admin "create") keep storing literal title/message. Legacy rows
-- keep their frozen title/message and render through the fallback path. No backfill.

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS title_key    VARCHAR(150) DEFAULT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS title_args   TEXT         DEFAULT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message_key  VARCHAR(150) DEFAULT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message_args TEXT         DEFAULT NULL;

-- System notifications no longer populate the literal `title` (key + args are the source of truth).
ALTER TABLE notifications ALTER COLUMN title DROP NOT NULL;
