-- ══════════════════════════════════════════════════════════════════════════════
-- Customer ID number (CCCD/CMND) for KYC on large gold buys / pawn — jewelry 4d
-- Nullable + additive: other shop types simply leave it blank. RLS already covers
-- the customers table; this is just a new optional column.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE customers ADD COLUMN IF NOT EXISTS id_number VARCHAR(50) DEFAULT NULL;
