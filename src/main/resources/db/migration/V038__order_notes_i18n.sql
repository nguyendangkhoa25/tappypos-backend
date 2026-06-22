-- Order notes/reasons i18n: system-generated split/merge/reject text on orders was stored as a
-- frozen Vietnamese string. Store a message key + JSON args for those system cases instead, so the
-- text renders in the reader's locale at read time (see OrderServiceImpl.mapToDTO). The literal
-- notes/cancel_reason/void_reason columns remain for USER-authored text and legacy rows (fallback).
-- No backfill.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS notes_key          VARCHAR(150) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS notes_args         TEXT         DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancel_reason_key  VARCHAR(150) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancel_reason_args TEXT         DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS void_reason_key    VARCHAR(150) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS void_reason_args   TEXT         DEFAULT NULL;
