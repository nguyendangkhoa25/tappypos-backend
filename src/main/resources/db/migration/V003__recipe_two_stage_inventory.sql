-- ============================================================
-- V003 — Recipe/BOM + two-stage inventory (định lượng + nguyên liệu → thành phẩm)
-- Bakery Phase 3 M1 (docs/BAKERY_PHASE3_RECIPE_INVENTORY_SPEC.md).
--
-- Additive only:
--   * product.product_kind defaults to 'FINISHED' → every existing product and all
--     other shop types are unchanged.
--   * Three new tenant tables (recipe / recipe_item / production_batch) with RLS.
--   * New RECIPE feature flag (catalog row); gating is opt-in per tenant/role.
-- Two STAGES, one MECHANISM: ingredient stock and finished-goods stock are both
-- rows in the existing `inventory` table; ingredients are products with kind=INGREDIENT.
-- ============================================================

-- ── 1. Product discriminator ────────────────────────────────
ALTER TABLE product
    ADD COLUMN IF NOT EXISTS product_kind VARCHAR(20) NOT NULL DEFAULT 'FINISHED';
-- FINISHED (sellable, default) | INGREDIENT (raw material, not sold) | BOTH
-- PostgreSQL has no ADD CONSTRAINT IF NOT EXISTS — guard for idempotency / replay.
DO $$ BEGIN
    ALTER TABLE product ADD CONSTRAINT chk_product_kind CHECK (product_kind IN ('FINISHED','INGREDIENT','BOTH'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
CREATE INDEX IF NOT EXISTS idx_product_kind
    ON product (tenant_id, product_kind) WHERE product_kind <> 'FINISHED';

-- ── 2. recipe (định lượng per finished product) ─────────────
CREATE TABLE IF NOT EXISTS recipe (
    id                  BIGSERIAL     PRIMARY KEY,
    tenant_id           VARCHAR(50)   NOT NULL,
    finished_product_id BIGINT        NOT NULL,
    yield_quantity      DECIMAL(12,3) NOT NULL DEFAULT 1,
    labor_cost          DECIMAL(15,2) NOT NULL DEFAULT 0,
    overhead_cost       DECIMAL(15,2) NOT NULL DEFAULT 0,
    notes               VARCHAR(500)  DEFAULT NULL,
    legacy_id           VARCHAR(50)   DEFAULT NULL,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP     DEFAULT NULL,
    created_by          VARCHAR(100)  DEFAULT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_recipe_yield   CHECK (yield_quantity > 0),
    CONSTRAINT uq_recipe_product  UNIQUE (finished_product_id, tenant_id),
    CONSTRAINT fk_recipe_product  FOREIGN KEY (finished_product_id) REFERENCES product(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_recipe_tenant       ON recipe (tenant_id);
CREATE INDEX IF NOT EXISTS idx_recipe_legacy_id    ON recipe (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
ALTER TABLE recipe ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipe FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON recipe
    USING (tenant_id = current_setting('app.current_tenant', true));

-- ── 3. recipe_item (an ingredient line of a recipe) ─────────
CREATE TABLE IF NOT EXISTS recipe_item (
    id                    BIGSERIAL     PRIMARY KEY,
    tenant_id             VARCHAR(50)   NOT NULL,
    recipe_id             BIGINT        NOT NULL,
    ingredient_product_id BIGINT        NOT NULL,
    quantity              DECIMAL(12,3) NOT NULL,
    unit                  VARCHAR(20)   DEFAULT NULL,
    legacy_id             VARCHAR(50)   DEFAULT NULL,
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP     DEFAULT NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_recipe_item_qty   CHECK (quantity > 0),
    CONSTRAINT fk_recipe_item_recipe FOREIGN KEY (recipe_id)             REFERENCES recipe(id)  ON DELETE CASCADE,
    CONSTRAINT fk_recipe_item_ingr   FOREIGN KEY (ingredient_product_id) REFERENCES product(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_recipe_item_recipe ON recipe_item (tenant_id, recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_item_legacy_id
    ON recipe_item (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
ALTER TABLE recipe_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipe_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON recipe_item
    USING (tenant_id = current_setting('app.current_tenant', true));

-- ── 4. production_batch (a "làm bánh" run) ──────────────────
CREATE TABLE IF NOT EXISTS production_batch (
    id                  BIGSERIAL     PRIMARY KEY,
    tenant_id           VARCHAR(50)   NOT NULL,
    finished_product_id BIGINT        NOT NULL,
    recipe_id           BIGINT        DEFAULT NULL,
    quantity_produced   DECIMAL(12,3) NOT NULL,
    ingredient_cost     DECIMAL(15,2) NOT NULL DEFAULT 0,
    unit_cost           DECIMAL(15,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    produced_by         VARCHAR(100)  DEFAULT NULL,
    notes               VARCHAR(500)  DEFAULT NULL,
    legacy_id           VARCHAR(50)   DEFAULT NULL,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP     DEFAULT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_prod_qty    CHECK (quantity_produced > 0),
    CONSTRAINT chk_prod_status CHECK (status IN ('COMPLETED','SPOILED')),
    CONSTRAINT fk_prod_product FOREIGN KEY (finished_product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_prod_recipe  FOREIGN KEY (recipe_id)           REFERENCES recipe(id)  ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_prod_tenant_date ON production_batch (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_prod_product     ON production_batch (tenant_id, finished_product_id);
CREATE INDEX IF NOT EXISTS idx_prod_legacy_id   ON production_batch (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
ALTER TABLE production_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE production_batch FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON production_batch
    USING (tenant_id = current_setting('app.current_tenant', true));

-- ── 5. RECIPE feature (catalog row; opt-in per tenant/role) ──
INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('RECIPE', 'Định Lượng & Sản Xuất',
        'Quản lý công thức/định lượng nguyên liệu cho thành phẩm, tính giá vốn thật và sản xuất (làm bánh) trừ kho nguyên liệu, cộng kho thành phẩm',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;
