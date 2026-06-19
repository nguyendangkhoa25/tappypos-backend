-- ══════════════════════════════════════════════════════════════════════════════
-- FnB modifier / topping groups (4b — slice 1: data model only)
-- Reusable add-on groups (size / đá / đường / topping) with per-option price deltas
-- and min/max select rules, linked to products. Nothing consumes these yet; the cart
-- integration lands in slice 2.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS modifier_groups (
    id          BIGSERIAL     PRIMARY KEY,
    tenant_id   VARCHAR(50)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    min_select  INT           NOT NULL DEFAULT 0,
    max_select  INT           NOT NULL DEFAULT 1,
    required    BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order  INT           NOT NULL DEFAULT 0,
    legacy_id   VARCHAR(50)   DEFAULT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS modifier_options (
    id                BIGSERIAL     PRIMARY KEY,
    tenant_id         VARCHAR(50)   NOT NULL,
    modifier_group_id BIGINT        NOT NULL REFERENCES modifier_groups(id),
    name              VARCHAR(150)  NOT NULL,
    price_delta       DECIMAL(15,2) NOT NULL DEFAULT 0,
    sort_order        INT           NOT NULL DEFAULT 0,
    legacy_id         VARCHAR(50)   DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP
);

-- Product ↔ modifier-group link (a group can be reused across many products).
CREATE TABLE IF NOT EXISTS product_modifier_groups (
    id                BIGSERIAL   PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL,
    product_id        BIGINT      NOT NULL,
    modifier_group_id BIGINT      NOT NULL REFERENCES modifier_groups(id),
    sort_order        INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,
    CONSTRAINT uq_product_modifier UNIQUE (tenant_id, product_id, modifier_group_id)
);

ALTER TABLE modifier_groups         ENABLE ROW LEVEL SECURITY;
ALTER TABLE modifier_groups         FORCE  ROW LEVEL SECURITY;
ALTER TABLE modifier_options        ENABLE ROW LEVEL SECURITY;
ALTER TABLE modifier_options        FORCE  ROW LEVEL SECURITY;
ALTER TABLE product_modifier_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_modifier_groups FORCE  ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'modifier_groups' AND policyname = 'modifier_groups_tenant_isolation') THEN
        CREATE POLICY modifier_groups_tenant_isolation ON modifier_groups
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'modifier_options' AND policyname = 'modifier_options_tenant_isolation') THEN
        CREATE POLICY modifier_options_tenant_isolation ON modifier_options
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'product_modifier_groups' AND policyname = 'product_modifier_groups_tenant_isolation') THEN
        CREATE POLICY product_modifier_groups_tenant_isolation ON product_modifier_groups
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_modifier_groups_legacy
    ON modifier_groups (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_modifier_options_group
    ON modifier_options (tenant_id, modifier_group_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_modifier_options_legacy
    ON modifier_options (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_product_modifier_groups_product
    ON product_modifier_groups (tenant_id, product_id);
