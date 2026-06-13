-- QR table ordering: new SUBMITTED order status (owner-confirmation gate) +
-- per-table QR token, plus the order columns that back the confirm step.
--
-- NOTE: only V001 exists on disk; this is the next sequential version. Before
-- deploying to an existing environment, confirm V002 has not already been
-- applied (spring.flyway.validate-on-migrate=false means a reused version is
-- silently skipped) — check the target DB's flyway_schema_history.

-- 1. Allow the new order status (orders.status has a CHECK constraint).
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('SUBMITTED','PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED'));

-- 2. Order columns backing QR customer orders + the confirm step.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmed_by VARCHAR(100) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_id BIGINT DEFAULT NULL;

-- 3. Per-table QR token (random, so QR URLs aren't enumerable).
ALTER TABLE shop_table ADD COLUMN IF NOT EXISTS qr_token VARCHAR(64) DEFAULT NULL;
UPDATE shop_table SET qr_token = gen_random_uuid()::text WHERE qr_token IS NULL;
CREATE INDEX IF NOT EXISTS idx_shop_table_qr_token
    ON shop_table (tenant_id, qr_token) WHERE qr_token IS NOT NULL;
