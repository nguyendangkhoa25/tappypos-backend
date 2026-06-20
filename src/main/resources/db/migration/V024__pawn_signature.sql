-- ══════════════════════════════════════════════════════════════════════════════
-- Digital pawn contract e-signature (PAWN_SHOP_TYPE_PLAN §4d · PAWN_DIGITAL_CONTRACT_SPEC, M1).
-- The borrower draws a signature on the device; it is stored in R2 and embedded into the
-- printed/PDF pawn contract. customer_signature_url holds the R2 public URL; signed_at doubles
-- as the "signed?" flag (NULL = unsigned).
--
-- Additive nullable columns on the existing RLS-protected `pawn` table (RLS + legacy_id already
-- present from V001). Gated by the existing PAWN feature — no new flag, no new table.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE pawn ADD COLUMN IF NOT EXISTS customer_signature_url VARCHAR(500) DEFAULT NULL;
ALTER TABLE pawn ADD COLUMN IF NOT EXISTS signed_at              TIMESTAMP    DEFAULT NULL;
