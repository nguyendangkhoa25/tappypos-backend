-- V009: Add TABLE_SERVICE feature and shop_table entity

INSERT INTO features (name, display_name, description)
VALUES ('TABLE_SERVICE', 'Quản Lý Bàn', 'Theo dõi trạng thái bàn và gọi món theo bàn cho quán ăn / quán nhậu')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS shop_table (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id        VARCHAR(50)  NOT NULL,
    table_number     VARCHAR(20)  NOT NULL,
    capacity         INT          NOT NULL DEFAULT 4,
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    current_order_id BIGINT       REFERENCES orders(id) ON DELETE SET NULL,
    location         VARCHAR(50),
    display_order    INT          NOT NULL DEFAULT 0,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE shop_table ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_table FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'shop_table' AND policyname = 'shop_table_rls'
    ) THEN
        CREATE POLICY shop_table_rls ON shop_table
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_shop_table_tenant
    ON shop_table (tenant_id, display_order);

CREATE INDEX IF NOT EXISTS idx_shop_table_order
    ON shop_table (current_order_id)
    WHERE current_order_id IS NOT NULL;
