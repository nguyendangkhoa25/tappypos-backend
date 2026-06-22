-- V041: Make combo_items a first-class tenant table (tenant_id + RLS + legacy_id).
--
-- combo_items was the one child table in V001 created WITHOUT a tenant_id column or RLS policy
-- (every sibling child — order_items, cart_items, loyalty_tiers, purchase_order_items, … — has both).
-- Isolation held only because combo_items is accessed exclusively as a JPA child collection of Combo
-- (which is tenant-filtered). Any future direct query / native join / reporting view on combo_items would
-- have leaked across tenants. This migration closes that gap. (tenant-isolation-reviewer MEDIUM, 2026-06-21 audit.)

-- 1. Add tenant_id, backfill from the parent combo, then enforce NOT NULL.
ALTER TABLE combo_items ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE combo_items ci
   SET tenant_id = c.tenant_id
  FROM combos c
 WHERE c.id = ci.combo_id
   AND ci.tenant_id IS NULL;

ALTER TABLE combo_items ALTER COLUMN tenant_id SET NOT NULL;

-- Safety net: a raw INSERT that omits tenant_id inherits the current RLS tenant context.
ALTER TABLE combo_items ALTER COLUMN tenant_id SET DEFAULT current_setting('app.current_tenant', true);

-- 2. External / legacy ID column + partial reverse-lookup index (per the legacy_id convention).
ALTER TABLE combo_items ADD COLUMN IF NOT EXISTS legacy_id VARCHAR(50) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_combo_items_legacy_id ON combo_items (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

-- 3. Row-Level Security — match the combos table (ENABLE + FORCE + tenant_isolation policy).
ALTER TABLE combo_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE combo_items FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON combo_items
    USING (tenant_id = current_setting('app.current_tenant', true));
