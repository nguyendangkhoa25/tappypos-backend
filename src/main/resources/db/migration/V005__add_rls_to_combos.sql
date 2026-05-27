-- V005 — Add Row Level Security to the combos table.
--
-- combos was created in V001 with tenant_id NOT NULL but was accidentally
-- omitted from the batch RLS loop. combo_items has no tenant_id column
-- and is accessed only through the combos FK, so no RLS is needed there.

ALTER TABLE combos ENABLE ROW LEVEL SECURITY;
ALTER TABLE combos FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON combos
    USING (tenant_id = current_tenant_id());
