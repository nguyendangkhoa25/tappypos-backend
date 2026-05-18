-- ============================================================
-- V001 — Initial schema: DDL + RLS + seed data (PostgreSQL)
--
-- Tenant isolation strategy:
--   tenant_id IS NULL         → master-level record
--   tenant_id = 'shopX'       → belongs to that shop
--   no tenant_id column       → global reference data
-- ============================================================

-- ── Helper: auto-update updated_at on every UPDATE ────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

-- ════════════════════════════════════════════════════════════
-- SECTION 1: Master-only tables (no tenant_id)
-- ════════════════════════════════════════════════════════════

-- 1.1 agents — agent / AGENT organisations
--     (renamed from platform_vendors to agents)
CREATE TABLE IF NOT EXISTS agents (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    contact_email VARCHAR(100) DEFAULT NULL,
    contact_phone VARCHAR(20)  DEFAULT NULL,
    notes         TEXT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    user_id       BIGINT       DEFAULT NULL,
    created_at    TIMESTAMP    DEFAULT NOW(),
    updated_at    TIMESTAMP    DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP    DEFAULT NULL
);

-- 1.2 tenants — one row per registered shop
CREATE TABLE IF NOT EXISTS tenants (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               VARCHAR(50)  NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    db_name                 VARCHAR(100) DEFAULT NULL,  -- unused in shared-DB mode; kept for reference
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    expiration_date         DATE         DEFAULT NULL,
    max_users               INT          DEFAULT NULL,
    features                TEXT         DEFAULT NULL,
    subscription_type       VARCHAR(50)  DEFAULT NULL,
    shop_type               VARCHAR(30)  DEFAULT NULL,
    contact_person_name     VARCHAR(100) DEFAULT NULL,
    contact_person_phone    VARCHAR(20)  DEFAULT NULL,
    contact_person_email    VARCHAR(100) DEFAULT NULL,
    contact_person_zalo_id  VARCHAR(50)  DEFAULT NULL,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT       NOT NULL,
    active_at               BIGINT       DEFAULT NULL,
    active_by               VARCHAR(100) DEFAULT NULL,
    created_by              VARCHAR(100) DEFAULT NULL,
    updated_by              VARCHAR(100) DEFAULT NULL,
    setup_complete           BOOLEAN      NOT NULL DEFAULT TRUE,
    vendor_id               BIGINT       DEFAULT NULL,
    CONSTRAINT fk_tenant_vendor FOREIGN KEY (vendor_id) REFERENCES agents (id) ON DELETE SET NULL,
    CONSTRAINT uq_tenants_tenant_id UNIQUE (tenant_id)
);

-- 1.3 user_feedback — all shop feedback aggregated here for master admin
CREATE TABLE IF NOT EXISTS user_feedback (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    username    VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    admin_note  VARCHAR(1000) DEFAULT NULL,
    resolved_at TIMESTAMP    DEFAULT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP    DEFAULT NULL
);

-- ════════════════════════════════════════════════════════════
-- SECTION 2: Global reference tables (no tenant_id)
-- ════════════════════════════════════════════════════════════

-- 2.1 banks — VietQR BIN reference (seeded once, read-only for shops)
CREATE TABLE IF NOT EXISTS banks (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL,
    bin        VARCHAR(10)  DEFAULT NULL,
    name       VARCHAR(255) NOT NULL,
    short_name VARCHAR(100) DEFAULT NULL,
    sort_order INT          NOT NULL DEFAULT 999,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW(),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_banks_code UNIQUE (code)
);

-- 2.2 gold_types and gold_brands removed: gold types → product categories, brands → vendors.

-- ════════════════════════════════════════════════════════════
-- SECTION 3: Unified tables (tenant_id nullable)
--   NULL  = master record
--   value = shop record
-- ════════════════════════════════════════════════════════════

-- 3.1 features
-- Platform-defined master data; no tenant_id — shared by all tenants, no RLS needed.
CREATE TABLE IF NOT EXISTS features (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description  VARCHAR(500) DEFAULT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    DEFAULT NOW(),
    updated_at   TIMESTAMP    DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_features_name UNIQUE (name)
);

-- 3.2 roles
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) DEFAULT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    created_at  TIMESTAMP    DEFAULT NOW(),
    updated_at  TIMESTAMP    DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP    DEFAULT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_name_master ON roles (name) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_name_tenant ON roles (name, tenant_id) WHERE tenant_id IS NOT NULL;

-- 3.3 users
CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               VARCHAR(100) DEFAULT NULL,
    username                VARCHAR(50)  NOT NULL,
    email                   VARCHAR(100) DEFAULT NULL,
    password                VARCHAR(255) NOT NULL,
    require_action          VARCHAR(50)  DEFAULT NULL,
    full_name               VARCHAR(100) DEFAULT NULL,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired     BOOLEAN      NOT NULL DEFAULT TRUE,
    notes                   VARCHAR(255) DEFAULT NULL,
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    avatar                  TEXT         DEFAULT NULL,
    color_preference        VARCHAR(50)  DEFAULT NULL,
    lang                    VARCHAR(10)  DEFAULT 'vi',
    preferences             TEXT         DEFAULT NULL,
    created_at              TIMESTAMP    DEFAULT NOW(),
    updated_at              TIMESTAMP    DEFAULT NOW(),
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP    DEFAULT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_master ON users (username) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_tenant ON users (username, tenant_id) WHERE tenant_id IS NOT NULL;

-- 3.4 user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id   BIGINT NOT NULL,
    role_id   BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- 3.5 role_features
CREATE TABLE IF NOT EXISTS role_features (
    id         BIGSERIAL PRIMARY KEY,
    role_id    BIGINT       NOT NULL,
    feature_id BIGINT       NOT NULL,
    created_at TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_role_feature UNIQUE (role_id, feature_id),
    CONSTRAINT fk_rf_role    FOREIGN KEY (role_id)    REFERENCES roles    (id) ON DELETE CASCADE,
    CONSTRAINT fk_rf_feature FOREIGN KEY (feature_id) REFERENCES features (id) ON DELETE CASCADE
);

-- 3.6 refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(500) NOT NULL,
    expiry_date BIGINT       NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  BIGINT       NOT NULL,
    updated_at  BIGINT       NOT NULL,
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 3.7 active_sessions
CREATE TABLE IF NOT EXISTS active_sessions (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) DEFAULT NULL,
    username    VARCHAR(50)  NOT NULL,
    session_id  VARCHAR(36)  NOT NULL,
    ip_address  VARCHAR(45)  DEFAULT NULL,
    user_agent  VARCHAR(500) DEFAULT NULL,
    login_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_active TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_sessions_master   ON active_sessions (username) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_sessions_tenant   ON active_sessions (username, tenant_id) WHERE tenant_id IS NOT NULL;

-- 3.8 notifications
CREATE TABLE IF NOT EXISTS notifications (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_id      VARCHAR(100) DEFAULT NULL,
    user_id        VARCHAR(50)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    message        TEXT         DEFAULT NULL,
    type           VARCHAR(30)  NOT NULL DEFAULT 'INFO',
    reference_type VARCHAR(50)  DEFAULT NULL,
    reference_id   BIGINT       DEFAULT NULL,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMP    DEFAULT NULL,
    created_by     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP    DEFAULT NULL
);

-- 3.9 notification_preferences (per-user opt-in list; 'ALL' = receive everything)
CREATE TABLE IF NOT EXISTS notification_preferences (
    id            BIGSERIAL     PRIMARY KEY,
    tenant_id     VARCHAR(100)  DEFAULT NULL,
    user_id       VARCHAR(50)   NOT NULL,
    enabled_types TEXT          NOT NULL DEFAULT 'ALL',
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP,
    CONSTRAINT uq_notif_pref_user UNIQUE (user_id, tenant_id)
);

-- 3.10 activity_log
CREATE TABLE IF NOT EXISTS activity_log (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) DEFAULT NULL,
    actor_username  VARCHAR(50)  NOT NULL,
    actor_full_name VARCHAR(100) DEFAULT NULL,
    action          VARCHAR(50)  NOT NULL,
    target_type     VARCHAR(50)  DEFAULT NULL,
    target_id       VARCHAR(100) DEFAULT NULL,
    description     VARCHAR(500) NOT NULL,
    ip_address      VARCHAR(45)  DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ════════════════════════════════════════════════════════════
-- SECTION 4: Tenant-only tables (tenant_id NOT NULL)
-- ════════════════════════════════════════════════════════════

-- 4.1 product_type
CREATE TABLE IF NOT EXISTS product_type (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_product_type_code_tenant UNIQUE (code, tenant_id)
);

-- 4.2 attribute_group
CREATE TABLE IF NOT EXISTS attribute_group (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    product_type_id BIGINT       NOT NULL,
    code            VARCHAR(50)  DEFAULT NULL,
    name            VARCHAR(255) NOT NULL,
    display_order   INT          DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP    DEFAULT NULL,
    CONSTRAINT fk_ag_product_type          FOREIGN KEY (product_type_id) REFERENCES product_type (id),
    CONSTRAINT uq_attr_group_code_type     UNIQUE (code, product_type_id)
);

-- 4.3 attribute_definition
CREATE TABLE IF NOT EXISTS attribute_definition (
    id                 BIGSERIAL    PRIMARY KEY,
    tenant_id          VARCHAR(100) NOT NULL,
    product_type_id    BIGINT       NOT NULL,
    attribute_group_id BIGINT       DEFAULT NULL,
    code               VARCHAR(100) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    data_type          VARCHAR(50)  NOT NULL,
    required           BOOLEAN      NOT NULL DEFAULT FALSE,
    searchable         BOOLEAN      NOT NULL DEFAULT FALSE,
    filterable         BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order      INT          DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_attr_def_code_type UNIQUE (code, product_type_id),
    CONSTRAINT fk_ad_product_type    FOREIGN KEY (product_type_id)    REFERENCES product_type    (id),
    CONSTRAINT fk_ad_attr_group      FOREIGN KEY (attribute_group_id) REFERENCES attribute_group (id)
);

-- 4.4 category
CREATE TABLE IF NOT EXISTS category (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  VARCHAR(100) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    parent_id  BIGINT       DEFAULT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP    DEFAULT NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
);

-- 4.5 vendors (shop suppliers — separate from agents)
CREATE TABLE IF NOT EXISTS vendors (
    id            BIGSERIAL    PRIMARY KEY,
    tenant_id     VARCHAR(100) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    contact_name  VARCHAR(100) DEFAULT NULL,
    email         VARCHAR(100) DEFAULT NULL,
    phone         VARCHAR(20)  DEFAULT NULL,
    address       VARCHAR(300) DEFAULT NULL,
    tax_id        VARCHAR(50)  DEFAULT NULL,
    payment_terms VARCHAR(20)  NOT NULL DEFAULT 'NET_30',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    notes         VARCHAR(500) DEFAULT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP    DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_vendors_code_tenant UNIQUE (code, tenant_id)
);

-- 4.6 product
CREATE TABLE IF NOT EXISTS product (
    id              BIGSERIAL      PRIMARY KEY,
    tenant_id       VARCHAR(100)   NOT NULL,
    product_type_id BIGINT         NOT NULL,
    sku             VARCHAR(100)   NOT NULL,
    name            VARCHAR(255)   NOT NULL,
    description     VARCHAR(1000)  DEFAULT NULL,
    price           DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    cost_price      DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    unit            VARCHAR(20)    DEFAULT NULL,
    vendor_id       BIGINT         DEFAULT NULL,
    status          VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP      DEFAULT NULL,
    barcode          VARCHAR(100)   DEFAULT NULL,
    shelf_location   VARCHAR(100)   DEFAULT NULL,
    legacy_id        VARCHAR(50)    DEFAULT NULL,
    commission_rate  DECIMAL(5,2)   DEFAULT NULL,
    CONSTRAINT uq_product_sku_tenant     UNIQUE (sku, tenant_id),
    CONSTRAINT fk_product_type           FOREIGN KEY (product_type_id) REFERENCES product_type (id),
    CONSTRAINT fk_product_vendor         FOREIGN KEY (vendor_id)       REFERENCES vendors       (id)
);

-- 4.7 product_category
CREATE TABLE IF NOT EXISTS product_category (
    product_id  BIGINT       NOT NULL,
    category_id BIGINT       NOT NULL,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT current_setting('app.current_tenant', true),
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_pc_product  FOREIGN KEY (product_id)  REFERENCES product  (id) ON DELETE CASCADE,
    CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);

-- 4.8 product_attribute_value
CREATE TABLE IF NOT EXISTS product_attribute_value (
    id            BIGSERIAL      PRIMARY KEY,
    tenant_id     VARCHAR(100)   NOT NULL,
    product_id    BIGINT         NOT NULL,
    attribute_id  BIGINT         NOT NULL,
    value_string  VARCHAR(1000)  DEFAULT NULL,
    value_number  DECIMAL(15,4)  DEFAULT NULL,
    value_boolean BOOLEAN        DEFAULT NULL,
    value_date    DATE           DEFAULT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_product_attribute UNIQUE (product_id, attribute_id),
    CONSTRAINT fk_pav_product    FOREIGN KEY (product_id)   REFERENCES product              (id) ON DELETE CASCADE,
    CONSTRAINT fk_pav_attribute  FOREIGN KEY (attribute_id) REFERENCES attribute_definition (id)
);

-- 4.9 variant_types
CREATE TABLE IF NOT EXISTS variant_types (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500) DEFAULT NULL,
    product_type_id BIGINT       DEFAULT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP    DEFAULT NULL
);

-- 4.10 variant_type_options
CREATE TABLE IF NOT EXISTS variant_type_options (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    variant_type_id BIGINT       NOT NULL,
    value           VARCHAR(100) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP    DEFAULT NULL,
    CONSTRAINT fk_vto_variant_type FOREIGN KEY (variant_type_id) REFERENCES variant_types (id)
);

-- 4.10b product_variants
CREATE TABLE IF NOT EXISTS product_variants (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL,
    product_id       BIGINT       NOT NULL,
    sku              VARCHAR(100) NOT NULL,
    barcode          VARCHAR(100) DEFAULT NULL,
    variant_options  JSONB        NOT NULL DEFAULT '{}',
    price_override   NUMERIC(15,2) DEFAULT NULL,
    cost_override    NUMERIC(15,2) DEFAULT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    DEFAULT NOW(),
    updated_at       TIMESTAMP    DEFAULT NOW(),
    deleted_at       TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_product_variants_sku UNIQUE (tenant_id, sku)
);

ALTER TABLE product_variants ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_variants FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON product_variants
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE INDEX IF NOT EXISTS idx_product_variants_product_id
    ON product_variants(product_id) WHERE deleted_at IS NULL;

-- 4.11 inventory
CREATE TABLE IF NOT EXISTS inventory (
    id                 BIGSERIAL      PRIMARY KEY,
    tenant_id          VARCHAR(100)   NOT NULL,
    product_id         BIGINT         NOT NULL,
    quantity_in_stock  BIGINT         NOT NULL DEFAULT 0,
    reorder_level      BIGINT         NOT NULL DEFAULT 10,
    reorder_quantity   BIGINT         NOT NULL DEFAULT 50,
    unit_cost          DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    warehouse_location VARCHAR(255)   NOT NULL DEFAULT 'Kho chính',
    zone               VARCHAR(50)    DEFAULT NULL,
    aisle              VARCHAR(20)    DEFAULT NULL,
    shelf              VARCHAR(20)    DEFAULT NULL,
    bin                VARCHAR(20)    DEFAULT NULL,
    last_restock_date  TIMESTAMP      DEFAULT NULL,
    expiry_date        DATE           DEFAULT NULL,
    batch_number       VARCHAR(100)   DEFAULT NULL,
    notes              VARCHAR(500)   DEFAULT NULL,
    status             VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    inventory_type     VARCHAR(50)    NOT NULL DEFAULT 'RETAIL',
    deleted            BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_inv_product       FOREIGN KEY (product_id) REFERENCES product (id)
);

-- 4.12 inventory_movement
CREATE TABLE IF NOT EXISTS inventory_movement (
    id               BIGSERIAL      PRIMARY KEY,
    tenant_id        VARCHAR(100)   NOT NULL,
    inventory_id     BIGINT         NOT NULL,
    movement_type    VARCHAR(50)    NOT NULL,
    quantity         DECIMAL(15,2)  NOT NULL,
    reference_number VARCHAR(100)   DEFAULT NULL,
    reference_type   VARCHAR(50)    DEFAULT NULL,
    created_by_user  VARCHAR(100)   DEFAULT NULL,
    reason           VARCHAR(255)   DEFAULT NULL,
    notes            VARCHAR(500)   DEFAULT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP      DEFAULT NULL,
    CONSTRAINT fk_im_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id) ON DELETE CASCADE
);

-- 4.13 customers
CREATE TABLE IF NOT EXISTS customers (
    id                          BIGSERIAL      PRIMARY KEY,
    tenant_id                   VARCHAR(100)   NOT NULL,
    name                        VARCHAR(100)   NOT NULL,
    phone                       VARCHAR(20)    NOT NULL,
    email                       VARCHAR(100)   DEFAULT NULL,
    notes                       TEXT           DEFAULT NULL,
    zalo_id                     VARCHAR(100)   DEFAULT NULL,
    facebook_id                 VARCHAR(100)   DEFAULT NULL,
    preferred_services          VARCHAR(500)   DEFAULT NULL,
    allergies_or_sensitivities  VARCHAR(500)   DEFAULT NULL,
    hair_type                   VARCHAR(100)   DEFAULT NULL,
    special_requests            VARCHAR(500)   DEFAULT NULL,
    id_card_number              VARCHAR(20)    DEFAULT NULL,
    date_of_birth               DATE           DEFAULT NULL,
    gender                      VARCHAR(10)    DEFAULT NULL,
    id_card_issued_date         DATE           DEFAULT NULL,
    id_card_issued_place        VARCHAR(255)   DEFAULT NULL,
    permanent_address           VARCHAR(500)   DEFAULT NULL,
    loyalty_points              INT            NOT NULL DEFAULT 0,
    total_spent                 DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    legacy_id                   VARCHAR(50)    DEFAULT NULL,
    created_at                  TIMESTAMP      DEFAULT NOW(),
    updated_at                  TIMESTAMP      DEFAULT NOW(),
    deleted                     BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_customers_phone_tenant    UNIQUE (phone, tenant_id),
    CONSTRAINT uq_customers_idcard_tenant   UNIQUE (id_card_number, tenant_id)
);

-- 4.14 invoice_buyers
CREATE TABLE IF NOT EXISTS invoice_buyers (
    id                 BIGSERIAL    PRIMARY KEY,
    tenant_id          VARCHAR(100) NOT NULL,
    customer_id        BIGINT       DEFAULT NULL,
    buyer_name         VARCHAR(255) DEFAULT NULL,
    buyer_legal_name   VARCHAR(255) DEFAULT NULL,
    buyer_tax_code     VARCHAR(50)  DEFAULT NULL,
    buyer_address      VARCHAR(500) DEFAULT NULL,
    buyer_phone_number VARCHAR(20)  DEFAULT NULL,
    buyer_email        VARCHAR(255) DEFAULT NULL,
    buyer_bank_name    VARCHAR(255) DEFAULT NULL,
    buyer_bank_account VARCHAR(50)  DEFAULT NULL,
    buyer_id_number    VARCHAR(50)  DEFAULT NULL,
    is_visiting_guest  BOOLEAN      DEFAULT FALSE
);

-- 4.15 orders
CREATE TABLE IF NOT EXISTS orders (
    id                      BIGSERIAL      PRIMARY KEY,
    tenant_id               VARCHAR(100)   NOT NULL,
    order_number            VARCHAR(20)    DEFAULT NULL,
    customer_id             BIGINT         DEFAULT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    payment_method          VARCHAR(50)    DEFAULT NULL,
    amount_paid             DECIMAL(10,2)  DEFAULT NULL,
    change_amount           DECIMAL(10,2)  DEFAULT NULL,
    total_amount            DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    discount_amount         DECIMAL(10,2)  DEFAULT 0.00,
    tax_percentage          DECIMAL(5,2)   DEFAULT 0.00,
    tax_amount              DECIMAL(10,2)  DEFAULT 0.00,
    commission_amount       DECIMAL(10,2)  DEFAULT 0.00,
    invoice_id              BIGINT         DEFAULT NULL,
    notes                   TEXT           DEFAULT NULL,
    created_by              VARCHAR(100)   DEFAULT NULL,
    completed_at            TIMESTAMP      DEFAULT NULL,
    completed_by            VARCHAR(100)   DEFAULT NULL,
    cancelled_at            TIMESTAMP      DEFAULT NULL,
    cancel_reason           VARCHAR(500)   DEFAULT NULL,
    cancelled_by            VARCHAR(100)   DEFAULT NULL,
    voided_at               TIMESTAMP      DEFAULT NULL,
    void_reason             VARCHAR(500)   DEFAULT NULL,
    voided_by               VARCHAR(100)   DEFAULT NULL,
    promotion_code          VARCHAR(50)    DEFAULT NULL,
    promotion_discount      DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    loyalty_points_redeemed INT            NOT NULL DEFAULT 0,
    loyalty_discount        DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    table_label             VARCHAR(100)   DEFAULT NULL,
    source                  VARCHAR(20)    NOT NULL DEFAULT 'POS',
    legacy_id               VARCHAR(50)    DEFAULT NULL,
    order_type              VARCHAR(20)    NOT NULL DEFAULT 'SELL',
    created_at              TIMESTAMP      DEFAULT NOW(),
    updated_at              TIMESTAMP      DEFAULT NOW(),
    deleted                 BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_orders_number_tenant  UNIQUE (order_number, tenant_id),
    CONSTRAINT chk_orders_status        CHECK  (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED')),
    CONSTRAINT chk_orders_order_type    CHECK  (order_type IN ('SELL', 'BUY', 'EXCHANGE')),
    CONSTRAINT fk_orders_customer       FOREIGN KEY (customer_id) REFERENCES customers (id)
);

-- 4.16 order_items
CREATE TABLE IF NOT EXISTS order_items (
    id                    BIGSERIAL      PRIMARY KEY,
    tenant_id             VARCHAR(100)   NOT NULL,
    order_id              BIGINT         NOT NULL,
    product_id            BIGINT         DEFAULT NULL,
    product_name          VARCHAR(255)   DEFAULT NULL,
    quantity              INT            NOT NULL DEFAULT 1,
    unit_price            DECIMAL(10,2)  NOT NULL,
    amount                DECIMAL(10,2)  NOT NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    tax_percentage        DECIMAL(5,2)   DEFAULT 0.00,
    tax_amount            DECIMAL(10,2)  DEFAULT 0.00,
    commission_rate       DECIMAL(5,2)   DEFAULT 0.00,
    commission_amount     DECIMAL(10,2)  DEFAULT 0.00,
    amount_before_tax     DECIMAL(10,2)  DEFAULT 0.00,
    assigned_employee_id   BIGINT         DEFAULT NULL,
    assigned_employee_name VARCHAR(255)   DEFAULT NULL,
    unit_cost              DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    cost_amount           DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    included_in_salary_id BIGINT         DEFAULT NULL,
    is_salary_calculated  BOOLEAN        NOT NULL DEFAULT FALSE,
    item_type             VARCHAR(20)    NOT NULL DEFAULT 'STANDARD',
    metadata              JSONB          DEFAULT NULL,
    completed_at          TIMESTAMP      DEFAULT NULL,
    created_at            TIMESTAMP      DEFAULT NOW(),
    updated_at            TIMESTAMP      DEFAULT NOW(),
    deleted               BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP      DEFAULT NULL,
    CONSTRAINT chk_oi_status    CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED')),
    CONSTRAINT chk_oi_item_type CHECK (item_type IN ('STANDARD', 'GOLD_IN', 'GOLD_OUT')),
    CONSTRAINT fk_oi_order      FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

-- 4.17 invoices
CREATE TABLE IF NOT EXISTS invoices (
    id                       BIGSERIAL      PRIMARY KEY,
    tenant_id                VARCHAR(100)   NOT NULL,
    order_id                 BIGINT         DEFAULT NULL,
    invoice_number           VARCHAR(50)    NOT NULL,
    invoice_series           VARCHAR(100)   DEFAULT NULL,
    total_amount             DECIMAL(10,2)  NOT NULL,
    tax                      DECIMAL(10,2)  DEFAULT 0.00,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    external_invoice_id      VARCHAR(100)   DEFAULT NULL,
    external_sync_at         TIMESTAMP      DEFAULT NULL,
    notes                    TEXT           DEFAULT NULL,
    issued_date              TIMESTAMP      DEFAULT NULL,
    total_amount_without_tax DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    tax_amount               DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    tax_percentage           DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    payment_type             VARCHAR(50)    DEFAULT NULL,
    invoice_type             VARCHAR(50)    DEFAULT NULL,
    currency_code            VARCHAR(3)     DEFAULT 'VND',
    error_message            VARCHAR(1000)  DEFAULT NULL,
    buyer_id                 BIGINT         DEFAULT NULL,
    buyer_name               VARCHAR(200)   DEFAULT NULL,
    buyer_legal_name         VARCHAR(200)   DEFAULT NULL,
    buyer_tax_code           VARCHAR(50)    DEFAULT NULL,
    buyer_address_line       VARCHAR(500)   DEFAULT NULL,
    buyer_phone_number       VARCHAR(20)    DEFAULT NULL,
    buyer_email              VARCHAR(200)   DEFAULT NULL,
    buyer_bank_name          VARCHAR(200)   DEFAULT NULL,
    buyer_bank_account       VARCHAR(50)    DEFAULT NULL,
    buyer_id_number          VARCHAR(50)    DEFAULT NULL,
    visiting_guest           BOOLEAN        NOT NULL DEFAULT FALSE,
    customer_id              BIGINT         DEFAULT NULL,
    code_of_tax              VARCHAR(100)   DEFAULT NULL,
    created_by               VARCHAR(100)   DEFAULT NULL,
    transaction_uuid         VARCHAR(255)   DEFAULT NULL,
    direction                VARCHAR(10)    NOT NULL DEFAULT 'OUTPUT',
    supplier_invoice_number  VARCHAR(50)    DEFAULT NULL,
    vendor_id                BIGINT         DEFAULT NULL,
    vendor_name              VARCHAR(200)   DEFAULT NULL,
    vendor_tax_code          VARCHAR(50)    DEFAULT NULL,
    purchase_order_id        BIGINT         DEFAULT NULL,
    deleted                  BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMP      DEFAULT NULL,
    created_at               TIMESTAMP      DEFAULT NOW(),
    updated_at               TIMESTAMP      DEFAULT NOW(),
    CONSTRAINT uq_invoices_number_tenant UNIQUE (invoice_number, tenant_id),
    CONSTRAINT chk_inv_status    CHECK (status IN ('DRAFT','COMPLETED','FAILED','CANCELLED')),
    CONSTRAINT chk_inv_direction CHECK (direction IN ('OUTPUT','INPUT')),
    CONSTRAINT fk_inv_order  FOREIGN KEY (order_id)  REFERENCES orders         (id),
    CONSTRAINT fk_inv_buyer  FOREIGN KEY (buyer_id)  REFERENCES invoice_buyers (id) ON DELETE SET NULL,
    CONSTRAINT fk_inv_vendor FOREIGN KEY (vendor_id) REFERENCES vendors        (id) ON DELETE SET NULL
    -- fk_inv_purchase_order deferred: purchase_orders is defined later in this file
);

-- 4.18 invoice_items
CREATE TABLE IF NOT EXISTS invoice_items (
    id                       BIGSERIAL      PRIMARY KEY,
    tenant_id                VARCHAR(100)   NOT NULL,
    invoice_id               BIGINT         NOT NULL,
    line_number              INT            DEFAULT NULL,
    order_item_id            BIGINT         DEFAULT NULL,
    service_name             VARCHAR(255)   DEFAULT NULL,
    service_code             VARCHAR(50)    DEFAULT NULL,
    unit                     VARCHAR(50)    DEFAULT NULL,
    unit_price               DECIMAL(19,2)  NOT NULL,
    quantity                 DECIMAL(19,2)  NOT NULL,
    discount                 DECIMAL(19,2)  DEFAULT 0.00,
    total_amount_without_tax DECIMAL(19,2)  NOT NULL,
    tax_percentage           DECIMAL(5,2)   DEFAULT 0.00,
    tax_amount               DECIMAL(19,2)  DEFAULT 0.00,
    total_amount_with_tax    DECIMAL(19,2)  NOT NULL,
    created_at               TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP      DEFAULT NOW(),
    deleted                  BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMP,
    CONSTRAINT chk_ii_amount   CHECK (total_amount_with_tax >= 0),
    CONSTRAINT fk_ii_invoice   FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
);

-- 4.19 carts
CREATE TABLE IF NOT EXISTS carts (
    id                 BIGSERIAL      PRIMARY KEY,
    tenant_id          VARCHAR(100)   NOT NULL,
    cart_id            VARCHAR(36)    NOT NULL,
    customer_id        BIGINT         DEFAULT NULL,
    subtotal           DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    total_discount     DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    total_tax          DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    total              DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    status             VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    applied_coupons    TEXT           DEFAULT NULL,
    applied_promotions TEXT           DEFAULT NULL,
    notes              TEXT           DEFAULT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    abandoned_at       TIMESTAMP      DEFAULT NULL,
    completed_at       TIMESTAMP      DEFAULT NULL,
    tax_rate           NUMERIC(5,4)   NOT NULL DEFAULT 0.10,
    CONSTRAINT uq_carts_id_tenant UNIQUE (cart_id, tenant_id)
);

-- 4.20 cart_items
CREATE TABLE IF NOT EXISTS cart_items (
    id              BIGSERIAL      PRIMARY KEY,
    tenant_id       VARCHAR(100)   NOT NULL,
    cart_id         BIGINT         NOT NULL,
    product_id      BIGINT         DEFAULT NULL,
    product_name    VARCHAR(255)   DEFAULT NULL,
    sku             VARCHAR(100)   DEFAULT NULL,
    barcode         VARCHAR(100)   DEFAULT NULL,
    quantity        INT            NOT NULL DEFAULT 1,
    unit_price      DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    base_price      DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    discount_type   VARCHAR(50)    DEFAULT NULL,
    discount_value  DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    discount_reason VARCHAR(255)   DEFAULT NULL,
    line_subtotal   DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    line_total      DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    tax             DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    line_grand_total DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    unit_cost       DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    item_type       VARCHAR(20)    NOT NULL DEFAULT 'STANDARD',
    metadata        JSONB          DEFAULT NULL,
    variants        JSONB          DEFAULT NULL,
    notes           TEXT           DEFAULT NULL,
    tax_rate               NUMERIC(5,4)   NOT NULL DEFAULT 0.10,
    assigned_employee_id   BIGINT         DEFAULT NULL,
    assigned_employee_name VARCHAR(255)   DEFAULT NULL,
    commission_rate        DECIMAL(5,2)   DEFAULT 0.00,
    commission_amount      DECIMAL(10,2)  DEFAULT 0.00,
    product_type_code      VARCHAR(50)    DEFAULT NULL,
    added_at               TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ci_item_type CHECK (item_type IN ('STANDARD', 'GOLD_IN', 'GOLD_OUT')),
    CONSTRAINT fk_ci_cart       FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE
);

-- 4.21 promotions
CREATE TABLE IF NOT EXISTS promotions (
    id                  BIGSERIAL      PRIMARY KEY,
    tenant_id           VARCHAR(100)   NOT NULL,
    name                VARCHAR(200)   NOT NULL,
    code                VARCHAR(50)    NOT NULL,
    type                VARCHAR(20)    NOT NULL,
    value               DECIMAL(10,2)  NOT NULL,
    min_order_amount    DECIMAL(10,2)  DEFAULT NULL,
    max_discount_amount DECIMAL(10,2)  DEFAULT NULL,
    start_date          TIMESTAMP      DEFAULT NULL,
    end_date            TIMESTAMP      DEFAULT NULL,
    usage_limit         INT            DEFAULT NULL,
    used_count          INT            NOT NULL DEFAULT 0,
    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    description         VARCHAR(500)   DEFAULT NULL,
    deleted             BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP      DEFAULT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_promotions_code_tenant UNIQUE (code, tenant_id)
);

-- 4.22 loyalty_programs (one row per tenant)
CREATE TABLE IF NOT EXISTS loyalty_programs (
    id                             BIGSERIAL      PRIMARY KEY,
    tenant_id                      VARCHAR(100)   NOT NULL,
    points_per_amount              INT            NOT NULL DEFAULT 1,
    amount_per_points              BIGINT         NOT NULL DEFAULT 10000,
    redemption_points_per_discount INT            NOT NULL DEFAULT 100,
    redemption_discount_amount     DECIMAL(10,2)  NOT NULL DEFAULT 10000,
    min_redemption_points          INT            NOT NULL DEFAULT 100,
    is_active                      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at                     TIMESTAMP      DEFAULT NOW(),
    updated_at                     TIMESTAMP      DEFAULT NOW(),
    deleted                        BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at                     TIMESTAMP      DEFAULT NULL
);

-- 4.23 loyalty_tiers
CREATE TABLE IF NOT EXISTS loyalty_tiers (
    id                BIGSERIAL      PRIMARY KEY,
    tenant_id         VARCHAR(100)   NOT NULL,
    name              VARCHAR(100)   NOT NULL,
    min_spend         DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    points_multiplier DECIMAL(5,2)   NOT NULL DEFAULT 1.00,
    color             VARCHAR(20)    DEFAULT '#9E9E9E',
    description       VARCHAR(500)   DEFAULT NULL,
    sort_order        INT            NOT NULL DEFAULT 0,
    created_at        TIMESTAMP      DEFAULT NOW(),
    updated_at        TIMESTAMP      DEFAULT NOW(),
    deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP      DEFAULT NULL
);

-- 4.24 loyalty_transactions
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_id      VARCHAR(100) NOT NULL,
    customer_id    BIGINT       NOT NULL,
    order_id       BIGINT       DEFAULT NULL,
    type           VARCHAR(20)  NOT NULL,
    points         INT          NOT NULL,
    balance_before INT          NOT NULL DEFAULT 0,
    balance_after  INT          NOT NULL DEFAULT 0,
    description    VARCHAR(500) DEFAULT NULL,
    created_at     TIMESTAMP    DEFAULT NOW(),
    updated_at     TIMESTAMP    DEFAULT NOW(),
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP    DEFAULT NULL,
    CONSTRAINT chk_lt_type   CHECK (type IN ('EARNED','REDEEMED','ADJUSTED','EXPIRED')),
    CONSTRAINT fk_lt_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_lt_order    FOREIGN KEY (order_id)    REFERENCES orders    (id)
);

-- 4.25 employees
CREATE TABLE IF NOT EXISTS employees (
    id              BIGSERIAL      PRIMARY KEY,
    tenant_id       VARCHAR(100)   NOT NULL,
    full_name       VARCHAR(255)   NOT NULL,
    phone           VARCHAR(50)    DEFAULT NULL,
    email           VARCHAR(255)   DEFAULT NULL,
    position        VARCHAR(50)    NOT NULL,
    department      VARCHAR(255)   DEFAULT NULL,
    hire_date       DATE           DEFAULT NULL,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    base_wage       DECIMAL(15,2)  DEFAULT NULL,
    commission_rate DECIMAL(5,2)   DEFAULT NULL,
    notes           TEXT           DEFAULT NULL,
    avatar          VARCHAR(512)   DEFAULT NULL,
    user_id                BIGINT         DEFAULT NULL,
    id_card_number         VARCHAR(20)    DEFAULT NULL,
    date_of_birth          DATE           DEFAULT NULL,
    gender                 VARCHAR(10)    DEFAULT NULL,
    permanent_address      TEXT           DEFAULT NULL,
    id_card_issued_date    DATE           DEFAULT NULL,
    id_card_issued_place   VARCHAR(255)   DEFAULT NULL,
    id_card_front_image    TEXT           DEFAULT NULL,
    id_card_back_image     TEXT           DEFAULT NULL,
    legacy_id              VARCHAR(50)    DEFAULT NULL,
    created_at             TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP      DEFAULT NOW(),
    deleted                BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at             TIMESTAMP      DEFAULT NULL,
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- 4.25b salary
CREATE TABLE IF NOT EXISTS salary (
    id               BIGSERIAL      PRIMARY KEY,
    tenant_id        VARCHAR(36)    NOT NULL,
    employee_id      BIGINT         NOT NULL,
    employee_name    VARCHAR(255)   NOT NULL,
    month            INT            NOT NULL,
    year             INT            NOT NULL,
    base_wage        DECIMAL(15,2)  NOT NULL DEFAULT 0,
    total_commission DECIMAL(15,2)  NOT NULL DEFAULT 0,
    advance_amount   DECIMAL(15,2)  NOT NULL DEFAULT 0,
    total_amount     DECIMAL(15,2)  NOT NULL DEFAULT 0,
    status           VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    notes            TEXT           DEFAULT NULL,
    approved_at      TIMESTAMP      DEFAULT NULL,
    paid_at          TIMESTAMP      DEFAULT NULL,
    created_by       VARCHAR(100)   DEFAULT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_salary_month  CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_salary_year   CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT chk_salary_status CHECK (status IN ('DRAFT','APPROVED','PAID')),
    CONSTRAINT uq_salary_emp_month_year UNIQUE (tenant_id, employee_id, month, year),
    CONSTRAINT fk_salary_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_salary_tenant_year_month ON salary (tenant_id, year DESC, month DESC);
CREATE INDEX IF NOT EXISTS idx_salary_employee          ON salary (tenant_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_salary_status            ON salary (tenant_id, status);

ALTER TABLE salary ENABLE ROW LEVEL SECURITY;
ALTER TABLE salary FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON salary
    USING (tenant_id = current_setting('app.current_tenant', true));

-- 4.25c salary_advance
CREATE TABLE IF NOT EXISTS salary_advance (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    employee_id     BIGINT          NOT NULL,
    employee_name   VARCHAR(255)    NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    advance_date    DATE            NOT NULL,
    note            TEXT            DEFAULT NULL,
    salary_id       BIGINT          DEFAULT NULL,
    is_deducted     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(100)    DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_adv_amount  CHECK (amount > 0),
    CONSTRAINT fk_adv_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adv_salary   FOREIGN KEY (salary_id)   REFERENCES salary(id)    ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_adv_employee ON salary_advance (tenant_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_adv_salary   ON salary_advance (salary_id);
CREATE INDEX IF NOT EXISTS idx_adv_date     ON salary_advance (tenant_id, advance_date DESC);

ALTER TABLE salary_advance ENABLE ROW LEVEL SECURITY;
ALTER TABLE salary_advance FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON salary_advance
    USING (tenant_id = current_setting('app.current_tenant', true));

-- 4.25d salary_adjustment
CREATE TABLE IF NOT EXISTS salary_adjustment (
    id          BIGSERIAL       PRIMARY KEY,
    tenant_id   VARCHAR(36)     NOT NULL,
    salary_id   BIGINT          NOT NULL,
    type        VARCHAR(20)     NOT NULL,
    amount      DECIMAL(15,2)   NOT NULL,
    note        TEXT            DEFAULT NULL,
    created_by  VARCHAR(100)    DEFAULT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_adj_type   CHECK (type IN ('BONUS', 'DEDUCTION')),
    CONSTRAINT chk_adj_amount CHECK (amount > 0),
    CONSTRAINT fk_adj_salary  FOREIGN KEY (salary_id) REFERENCES salary(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_adj_salary ON salary_adjustment (salary_id);

ALTER TABLE salary_adjustment ENABLE ROW LEVEL SECURITY;
ALTER TABLE salary_adjustment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON salary_adjustment
    USING (tenant_id = current_setting('app.current_tenant', true));

-- FK from order_items.included_in_salary_id → salary (deferred because order_items is defined earlier)
ALTER TABLE order_items
    ADD CONSTRAINT fk_oi_salary FOREIGN KEY (included_in_salary_id) REFERENCES salary(id) ON DELETE SET NULL;

-- 4.26 purchase_orders
CREATE TABLE IF NOT EXISTS purchase_orders (
    id            BIGSERIAL      PRIMARY KEY,
    tenant_id     VARCHAR(100)   NOT NULL,
    po_number     VARCHAR(30)    NOT NULL,
    vendor_id     BIGINT         NOT NULL,
    status        VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    total_amount  DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    expected_date DATE           DEFAULT NULL,
    ordered_at    TIMESTAMP      DEFAULT NULL,
    received_at   TIMESTAMP      DEFAULT NULL,
    created_by    VARCHAR(100)   DEFAULT NULL,
    notes         VARCHAR(500)   DEFAULT NULL,
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP      DEFAULT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_po_number_tenant UNIQUE (po_number, tenant_id),
    CONSTRAINT fk_po_vendor        FOREIGN KEY (vendor_id) REFERENCES vendors (id)
);

-- Deferred FK: invoices.purchase_order_id → purchase_orders (purchase_orders defined after invoices)
ALTER TABLE invoices
    ADD CONSTRAINT fk_inv_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE SET NULL;

-- 4.27 purchase_order_items
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id                BIGSERIAL      PRIMARY KEY,
    tenant_id         VARCHAR(100)   NOT NULL,
    purchase_order_id BIGINT         NOT NULL,
    product_id        BIGINT         DEFAULT NULL,
    product_name      VARCHAR(255)   NOT NULL,
    product_sku       VARCHAR(100)   DEFAULT NULL,
    quantity_ordered  INT            NOT NULL,
    quantity_received INT            NOT NULL DEFAULT 0,
    unit_cost         DECIMAL(15,2)  NOT NULL,
    total_cost        DECIMAL(15,2)  NOT NULL,
    deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP      DEFAULT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      DEFAULT NULL,
    CONSTRAINT fk_poi_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id)
);

-- 4.28 market_prices
CREATE TABLE IF NOT EXISTS market_prices (
    id         BIGSERIAL      PRIMARY KEY,
    tenant_id  VARCHAR(100)   NOT NULL,
    name       VARCHAR(100)   NOT NULL,
    unit       VARCHAR(20)    NOT NULL,
    buy_price  DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    sell_price DECIMAL(15,2)  DEFAULT NULL,
    is_active  BOOLEAN        NOT NULL DEFAULT TRUE,
    notes      VARCHAR(500)   DEFAULT NULL,
    sort_order INT            NOT NULL DEFAULT 999,
    created_at TIMESTAMP      DEFAULT NOW(),
    updated_at TIMESTAMP      DEFAULT NOW(),
    deleted    BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP      DEFAULT NULL
);

-- 4.29 buyback_orders
CREATE TABLE IF NOT EXISTS buyback_orders (
    id             BIGSERIAL      PRIMARY KEY,
    tenant_id      VARCHAR(100)   NOT NULL,
    order_number   VARCHAR(30)    NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    customer_id    BIGINT         DEFAULT NULL,
    customer_name  VARCHAR(100)   DEFAULT NULL,
    customer_phone VARCHAR(20)    DEFAULT NULL,
    payment_method VARCHAR(20)    NOT NULL DEFAULT 'CASH',
    buy_total      DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    sale_total     DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    net_amount     DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    notes          VARCHAR(500)   DEFAULT NULL,
    created_by     VARCHAR(100)   DEFAULT NULL,
    completed_at   TIMESTAMP      DEFAULT NULL,
    completed_by   VARCHAR(100)   DEFAULT NULL,
    cancelled_at   TIMESTAMP      DEFAULT NULL,
    cancelled_by   VARCHAR(100)   DEFAULT NULL,
    created_at     TIMESTAMP      DEFAULT NOW(),
    updated_at     TIMESTAMP      DEFAULT NOW(),
    deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_bb_order_number_tenant UNIQUE (order_number, tenant_id)
);

-- 4.30 buyback_order_items
CREATE TABLE IF NOT EXISTS buyback_order_items (
    id               BIGSERIAL      PRIMARY KEY,
    tenant_id        VARCHAR(100)   NOT NULL,
    buyback_order_id BIGINT         NOT NULL,
    item_type        VARCHAR(10)    NOT NULL,
    commodity_id     BIGINT         DEFAULT NULL,
    commodity_name   VARCHAR(100)   DEFAULT NULL,
    unit             VARCHAR(20)    DEFAULT NULL,
    weight           DECIMAL(10,3)  DEFAULT NULL,
    condition_type   VARCHAR(20)    DEFAULT NULL,
    price_per_unit   DECIMAL(15,2)  DEFAULT NULL,
    product_name     VARCHAR(255)   DEFAULT NULL,
    quantity         INT            DEFAULT NULL,
    unit_price       DECIMAL(15,2)  DEFAULT NULL,
    total_price      DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    gold_type        VARCHAR(50)    DEFAULT NULL,
    gold_brand       VARCHAR(100)   DEFAULT NULL,
    gold_weight      DECIMAL(10,3)  DEFAULT NULL,
    gem_weight       DECIMAL(10,3)  DEFAULT NULL,
    proc_price       DECIMAL(15,2)  DEFAULT NULL,
    item_mode        VARCHAR(20)    DEFAULT NULL,
    notes            VARCHAR(500)   DEFAULT NULL,
    created_at       TIMESTAMP      DEFAULT NOW(),
    updated_at       TIMESTAMP      DEFAULT NOW(),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP      DEFAULT NULL,
    CONSTRAINT fk_boi_order FOREIGN KEY (buyback_order_id) REFERENCES buyback_orders (id) ON DELETE CASCADE
);

-- 4.31 pawn
CREATE TABLE IF NOT EXISTS pawn (
    pawn_id                  BIGSERIAL      PRIMARY KEY,
    tenant_id                VARCHAR(100)   NOT NULL,
    customer_id              BIGINT         DEFAULT NULL,
    item_name                VARCHAR(255)   DEFAULT NULL,
    item_description         TEXT           DEFAULT NULL,
    item_weight              DECIMAL(10,3)  DEFAULT NULL,
    gem_weight               DECIMAL(10,3)  DEFAULT NULL,
    item_value               DECIMAL(15,2)  DEFAULT NULL,
    item_type                VARCHAR(100)   DEFAULT NULL,
    item_brand               VARCHAR(100)   DEFAULT NULL,
    pawn_date                TIMESTAMP      DEFAULT NULL,
    pawn_due_date            TIMESTAMP      DEFAULT NULL,
    pawn_amount              DECIMAL(15,2)  DEFAULT NULL,
    interest_rate            DECIMAL(10,4)  DEFAULT NULL,
    status                   VARCHAR(50)    DEFAULT NULL,
    created_by               VARCHAR(100)   DEFAULT NULL,
    created_at               TIMESTAMP      DEFAULT NULL,
    updated_by               VARCHAR(100)   DEFAULT NULL,
    updated_at               TIMESTAMP      DEFAULT NULL,
    canceled_reason          TEXT           DEFAULT NULL,
    total_amount             DECIMAL(15,2)  DEFAULT NULL,
    redeem_date              TIMESTAMP      DEFAULT NULL,
    interest_amount          DECIMAL(15,2)  DEFAULT NULL,
    forfeited_reason         TEXT           DEFAULT NULL,
    forfeited_amount         DECIMAL(15,2)  DEFAULT NULL,
    forfeited_date           TIMESTAMP      DEFAULT NULL,
    original_id              BIGINT         DEFAULT NULL,
    interest_calc_mode       VARCHAR(20)    DEFAULT 'DAILY_30',
    pawned_days              INT            DEFAULT NULL,
    visible                  BOOLEAN        NOT NULL DEFAULT TRUE,
    pawn_category            VARCHAR(50)    DEFAULT NULL,
    legacy_id                VARCHAR(50)    DEFAULT NULL,
    customer_name            VARCHAR(255)   DEFAULT NULL
);

-- 4.32 pawn_audit
CREATE TABLE IF NOT EXISTS pawn_audit (
    action_id                BIGSERIAL      PRIMARY KEY,
    tenant_id                VARCHAR(100)   NOT NULL,
    action_type              VARCHAR(100)   DEFAULT NULL,
    action_time              TIMESTAMP      DEFAULT NULL,
    pawn_id                  BIGINT         DEFAULT NULL,
    customer_id              BIGINT         DEFAULT NULL,
    item_name                VARCHAR(255)   DEFAULT NULL,
    item_description         TEXT           DEFAULT NULL,
    item_weight              DECIMAL(10,3)  DEFAULT NULL,
    gem_weight               DECIMAL(10,3)  DEFAULT NULL,
    item_value               DECIMAL(15,2)  DEFAULT NULL,
    item_type                VARCHAR(100)   DEFAULT NULL,
    item_brand               VARCHAR(100)   DEFAULT NULL,
    pawn_date                TIMESTAMP      DEFAULT NULL,
    pawn_due_date            TIMESTAMP      DEFAULT NULL,
    pawn_amount              DECIMAL(15,2)  DEFAULT NULL,
    interest_rate            DECIMAL(10,4)  DEFAULT NULL,
    status                   VARCHAR(50)    DEFAULT NULL,
    canceled_reason          TEXT           DEFAULT NULL,
    total_amount             DECIMAL(15,2)  DEFAULT NULL,
    redeem_date              TIMESTAMP      DEFAULT NULL,
    interest_amount          DECIMAL(15,2)  DEFAULT NULL,
    forfeited_reason         TEXT           DEFAULT NULL,
    forfeited_amount         DECIMAL(15,2)  DEFAULT NULL,
    forfeited_date           TIMESTAMP      DEFAULT NULL,
    original_id              BIGINT         DEFAULT NULL,
    interest_calc_mode       VARCHAR(20)    DEFAULT NULL,
    created_by               VARCHAR(100)   DEFAULT NULL,
    created_at               TIMESTAMP      DEFAULT NULL,
    updated_by               VARCHAR(100)   DEFAULT NULL,
    updated_at               TIMESTAMP      DEFAULT NULL
);

-- 4.33 pawn_req_money
CREATE TABLE IF NOT EXISTS pawn_req_money (
    request_id     BIGSERIAL      PRIMARY KEY,
    tenant_id      VARCHAR(100)   NOT NULL,
    pawn_id        BIGINT         DEFAULT NULL,
    request_amount DECIMAL(15,2)  DEFAULT NULL,
    request_date   TIMESTAMP      DEFAULT NULL,
    created_by     VARCHAR(100)   DEFAULT NULL,
    created_at     TIMESTAMP      DEFAULT NULL,
    updated_by     VARCHAR(100)   DEFAULT NULL,
    updated_at     TIMESTAMP      DEFAULT NULL,
    legacy_id      VARCHAR(50)    DEFAULT NULL
);

-- 4.34 pawn_req_money_audit
CREATE TABLE IF NOT EXISTS pawn_req_money_audit (
    action_id      BIGSERIAL      PRIMARY KEY,
    tenant_id      VARCHAR(100)   NOT NULL,
    action_type    VARCHAR(100)   DEFAULT NULL,
    action_time    TIMESTAMP      DEFAULT NULL,
    request_id     BIGINT         DEFAULT NULL,
    pawn_id        BIGINT         DEFAULT NULL,
    request_amount DECIMAL(15,2)  DEFAULT NULL,
    request_date   TIMESTAMP      DEFAULT NULL,
    created_by     VARCHAR(100)   DEFAULT NULL,
    created_at     TIMESTAMP      DEFAULT NULL,
    updated_by     VARCHAR(100)   DEFAULT NULL,
    updated_at     TIMESTAMP      DEFAULT NULL
);

-- 4.35 pawn_item_electronics
CREATE TABLE IF NOT EXISTS pawn_item_electronics (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    pawn_id     BIGINT       NOT NULL UNIQUE REFERENCES pawn(pawn_id) ON DELETE CASCADE,
    brand       VARCHAR(100),
    model       VARCHAR(100),
    imei        VARCHAR(100),
    storage     VARCHAR(50),
    color       VARCHAR(50),
    condition   VARCHAR(50)
);

-- 4.36 pawn_item_vehicle (covers MOTORBIKE + CAR)
CREATE TABLE IF NOT EXISTS pawn_item_vehicle (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_id      VARCHAR(100) NOT NULL,
    pawn_id        BIGINT       NOT NULL UNIQUE REFERENCES pawn(pawn_id) ON DELETE CASCADE,
    brand          VARCHAR(100),
    model          VARCHAR(100),
    year           INTEGER,
    license_plate  VARCHAR(20),
    engine_number  VARCHAR(100),
    chassis_number VARCHAR(100),
    color          VARCHAR(50),
    condition      VARCHAR(50)
);

-- 4.37 pawn_item_watch
CREATE TABLE IF NOT EXISTS pawn_item_watch (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    pawn_id     BIGINT       NOT NULL UNIQUE REFERENCES pawn(pawn_id) ON DELETE CASCADE,
    brand       VARCHAR(100),
    model       VARCHAR(100),
    material    VARCHAR(100),
    condition   VARCHAR(50)
);

-- 4.38 pawn_item_real_estate
CREATE TABLE IF NOT EXISTS pawn_item_real_estate (
    id                 BIGSERIAL    PRIMARY KEY,
    tenant_id          VARCHAR(100) NOT NULL,
    pawn_id            BIGINT       NOT NULL UNIQUE REFERENCES pawn(pawn_id) ON DELETE CASCADE,
    certificate_number VARCHAR(100),
    certificate_type   VARCHAR(50),
    owner_name         VARCHAR(200),
    address            TEXT,
    area_sqm           DECIMAL(12,2),
    condition          VARCHAR(50)
);

-- 4.39 pawn_item_general (miscellaneous items)
CREATE TABLE IF NOT EXISTS pawn_item_general (
    id            BIGSERIAL    PRIMARY KEY,
    tenant_id     VARCHAR(100) NOT NULL,
    pawn_id       BIGINT       NOT NULL UNIQUE REFERENCES pawn(pawn_id) ON DELETE CASCADE,
    serial_number VARCHAR(100),
    condition     VARCHAR(50)
);

-- 4.40 shop_info
CREATE TABLE IF NOT EXISTS shop_info (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL,
    shop_name        VARCHAR(100) NOT NULL DEFAULT 'Cửa hàng của tôi',
    address          VARCHAR(500) DEFAULT '',
    company_name     VARCHAR(100) DEFAULT '',
    phone            VARCHAR(20)  DEFAULT '',
    email            VARCHAR(100) DEFAULT '',
    supplier_tax_code VARCHAR(150) DEFAULT '',
    website          VARCHAR(200) DEFAULT '',
    created_at       TIMESTAMP    DEFAULT NOW(),
    updated_at       TIMESTAMP    DEFAULT NOW(),
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP    DEFAULT NULL
);

-- 4.36 shop_config
CREATE TABLE IF NOT EXISTS shop_config (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    VARCHAR(100) NOT NULL,
    config_key   VARCHAR(100) NOT NULL,
    config_value TEXT         DEFAULT NULL,
    config_group VARCHAR(50)  DEFAULT NULL,
    encrypted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_shop_config_key_tenant UNIQUE (config_key, tenant_id)
);

-- 4.37 print_templates
CREATE TABLE IF NOT EXISTS print_templates (
    id            BIGSERIAL    PRIMARY KEY,
    tenant_id     VARCHAR(100) NOT NULL,
    template_type VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL DEFAULT 'Mặc định',
    config_json   TEXT         NOT NULL,
    is_default    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    DEFAULT NOW(),
    updated_at    TIMESTAMP    DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP    DEFAULT NULL,
    CONSTRAINT uq_print_templates_type_name_tenant UNIQUE (template_type, name, tenant_id)
);

-- 4.38 bank_accounts
CREATE TABLE IF NOT EXISTS bank_accounts (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    bank_bin        VARCHAR(20)  NOT NULL,
    bank_code       VARCHAR(20)  NOT NULL,
    bank_name       VARCHAR(255) NOT NULL,
    bank_short_name VARCHAR(100) DEFAULT NULL,
    account_number  VARCHAR(50)  NOT NULL,
    account_name    VARCHAR(255) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    DEFAULT NULL,
    updated_at      TIMESTAMP    DEFAULT NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP    DEFAULT NULL
);

-- 4.39 shop_expense
CREATE TABLE IF NOT EXISTS shop_expense (
    id               BIGSERIAL      PRIMARY KEY,
    tenant_id        VARCHAR(100)   NOT NULL,
    amount           DECIMAL(20,0)  NOT NULL,
    category         VARCHAR(30)    NOT NULL,
    description      VARCHAR(500)   DEFAULT NULL,
    expense_date     DATE           NOT NULL,
    payment_method   VARCHAR(20)    DEFAULT NULL,
    reference_number VARCHAR(100)   DEFAULT NULL,
    created_by       VARCHAR(100)   DEFAULT NULL,
    updated_by       VARCHAR(100)   DEFAULT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      DEFAULT NOW(),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP      DEFAULT NULL
);

-- 4.40 api_audit_log
CREATE TABLE IF NOT EXISTS api_audit_log (
    log_id                BIGSERIAL    PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL,
    trace_id              VARCHAR(100) NOT NULL,
    api_endpoint          VARCHAR(500) NOT NULL,
    http_method           VARCHAR(20)  NOT NULL,
    request_body          TEXT         DEFAULT NULL,
    request_headers       TEXT         DEFAULT NULL,
    response_body         TEXT         DEFAULT NULL,
    response_headers      TEXT         DEFAULT NULL,
    response_status       INT          DEFAULT NULL,
    request_size          BIGINT       DEFAULT NULL,
    response_size         BIGINT       DEFAULT NULL,
    execution_time_ms     BIGINT       DEFAULT NULL,
    error_message         TEXT         DEFAULT NULL,
    exception_stack_trace TEXT         DEFAULT NULL,
    user_id               VARCHAR(100) DEFAULT NULL,
    ip_address            VARCHAR(50)  DEFAULT NULL,
    status                VARCHAR(20)  DEFAULT NULL,
    description           VARCHAR(255) DEFAULT NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP    DEFAULT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 4.41 gold_price (per-shop price board)
CREATE TABLE IF NOT EXISTS gold_price (
    id            BIGSERIAL      PRIMARY KEY,
    tenant_id     VARCHAR(100)   NOT NULL,
    code          VARCHAR(50)    NOT NULL,
    label         VARCHAR(100)   NOT NULL,
    buy           DECIMAL(20,0)  NOT NULL DEFAULT 0,
    sell          DECIMAL(20,0)  NOT NULL DEFAULT 0,
    pawn          DECIMAL(20,0)  NOT NULL DEFAULT 0,
    display_order INT            NOT NULL DEFAULT 10,
    note          VARCHAR(500)   DEFAULT NULL,
    show_in_board BOOLEAN        NOT NULL DEFAULT TRUE,
    category_id   BIGINT         DEFAULT NULL REFERENCES category(id) ON DELETE SET NULL,
    vendor_price  NUMERIC(20,0)  DEFAULT NULL,
    created_by    VARCHAR(100)   DEFAULT NULL,
    updated_by    VARCHAR(100)   DEFAULT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP      DEFAULT NULL
);

-- 4.42 jewelry_counters (per-shop display case configuration)
CREATE TABLE IF NOT EXISTS jewelry_counters (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  VARCHAR(100) NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(250) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_jewelry_counters_code_tenant UNIQUE (code, tenant_id)
);

-- 4.43 contact_leads (trial registration requests from the public landing page)
CREATE TABLE IF NOT EXISTS contact_leads (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    phone       VARCHAR(20)   NOT NULL,
    shop_type   VARCHAR(50),
    note        VARCHAR(1000),
    source      VARCHAR(50)   DEFAULT 'LANDING_PAGE',
    status      VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    admin_note  VARCHAR(1000),
    deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- 4.44 product_catalog (master shared product catalog; no RLS — global reference data)
CREATE TABLE IF NOT EXISTS product_catalog (
    id            BIGSERIAL     PRIMARY KEY,
    barcode       VARCHAR(100)  NOT NULL UNIQUE,
    name          VARCHAR(200)  NOT NULL,
    brand         VARCHAR(100),
    category_hint VARCHAR(100),
    unit          VARCHAR(50)   DEFAULT 'Cái',
    description   VARCHAR(1000),
    image_url     VARCHAR(500),
    source        VARCHAR(50)   DEFAULT 'MANUAL',
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

-- 4.45 shop_integrations (per-tenant third-party integration credentials)
CREATE TABLE IF NOT EXISTS shop_integrations (
    id                BIGSERIAL    PRIMARY KEY,
    tenant_id         VARCHAR(100) NOT NULL,
    integration_type  VARCHAR(50)  NOT NULL,
    config_json       TEXT         DEFAULT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DISCONNECTED',
    connected_at      TIMESTAMP    DEFAULT NULL,
    disconnected_at   TIMESTAMP    DEFAULT NULL,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP    DEFAULT NULL,
    created_at        TIMESTAMP    DEFAULT NOW(),
    updated_at        TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_shop_integrations_tenant_type UNIQUE (tenant_id, integration_type)
);

ALTER TABLE shop_integrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_integrations FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON shop_integrations
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE INDEX IF NOT EXISTS idx_shop_integrations_tenant_type
    ON shop_integrations (tenant_id, integration_type);

-- 4.46 entity_images (Drive file references for any entity)
CREATE TABLE IF NOT EXISTS entity_images (
    id             BIGSERIAL     PRIMARY KEY,
    tenant_id      VARCHAR(100)  NOT NULL,
    entity_type    VARCHAR(30)   NOT NULL,
    entity_id      BIGINT        NOT NULL,
    drive_file_id  VARCHAR(200)  NOT NULL,
    drive_url      TEXT          DEFAULT NULL,
    thumbnail_url  TEXT          DEFAULT NULL,
    label          VARCHAR(100)  DEFAULT NULL,
    uploaded_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP     DEFAULT NULL,
    created_at     TIMESTAMP     DEFAULT NOW(),
    updated_at     TIMESTAMP     DEFAULT NOW()
);

ALTER TABLE entity_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE entity_images FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON entity_images
    USING (tenant_id = current_setting('app.current_tenant', true));

CREATE INDEX IF NOT EXISTS idx_entity_images_entity
    ON entity_images (tenant_id, entity_type, entity_id)
    WHERE deleted = FALSE;

-- ════════════════════════════════════════════════════════════
-- SECTION 5: Indexes
-- ════════════════════════════════════════════════════════════

-- agents
CREATE INDEX IF NOT EXISTS idx_agents_active    ON agents (active);
CREATE INDEX IF NOT EXISTS idx_agents_deleted   ON agents (deleted);

-- tenants
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_id  ON tenants (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_active      ON tenants (active);
CREATE INDEX IF NOT EXISTS idx_tenants_vendor_id   ON tenants (vendor_id);

-- user_feedback
CREATE INDEX IF NOT EXISTS idx_uf_tenant   ON user_feedback (tenant_id);
CREATE INDEX IF NOT EXISTS idx_uf_username ON user_feedback (username);
CREATE INDEX IF NOT EXISTS idx_uf_status   ON user_feedback (status);
CREATE INDEX IF NOT EXISTS idx_uf_deleted  ON user_feedback (deleted);

-- banks
CREATE INDEX IF NOT EXISTS idx_banks_sort_order ON banks (sort_order);
CREATE INDEX IF NOT EXISTS idx_banks_active     ON banks (is_active);

-- features
CREATE INDEX IF NOT EXISTS idx_features_active     ON features (active);
CREATE INDEX IF NOT EXISTS idx_features_deleted    ON features (deleted);

-- roles
CREATE INDEX IF NOT EXISTS idx_roles_tenant_id ON roles (tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_deleted   ON roles (deleted);

-- users
CREATE INDEX IF NOT EXISTS idx_users_tenant_id  ON users (tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_active     ON users (active);
CREATE INDEX IF NOT EXISTS idx_users_active_uname ON users (active, username);

-- user_roles
CREATE INDEX IF NOT EXISTS idx_ur_user_id   ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_ur_role_id   ON user_roles (role_id);

-- role_features
CREATE INDEX IF NOT EXISTS idx_rf_role_id    ON role_features (role_id);
CREATE INDEX IF NOT EXISTS idx_rf_feature_id ON role_features (feature_id);

-- refresh_tokens
CREATE INDEX IF NOT EXISTS idx_rt_user_id   ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_rt_active    ON refresh_tokens (active);

-- active_sessions
CREATE INDEX IF NOT EXISTS idx_as_tenant_id ON active_sessions (tenant_id);

-- notifications
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read, deleted);
CREATE INDEX IF NOT EXISTS idx_notif_tenant_id ON notifications (tenant_id);

-- notification_preferences
CREATE INDEX IF NOT EXISTS idx_np_user_id ON notification_preferences (user_id);

-- activity_log
CREATE INDEX IF NOT EXISTS idx_al_actor      ON activity_log (actor_username);
CREATE INDEX IF NOT EXISTS idx_al_action     ON activity_log (action);
CREATE INDEX IF NOT EXISTS idx_al_created_at ON activity_log (created_at);
CREATE INDEX IF NOT EXISTS idx_al_tenant_id  ON activity_log (tenant_id);

-- product_type
CREATE INDEX IF NOT EXISTS idx_pt_tenant_id  ON product_type (tenant_id);
CREATE INDEX IF NOT EXISTS idx_pt_deleted    ON product_type (deleted);

-- product
CREATE INDEX IF NOT EXISTS idx_product_tenant_id  ON product (tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_status     ON product (status);
CREATE INDEX IF NOT EXISTS idx_product_deleted    ON product (deleted);
CREATE INDEX IF NOT EXISTS idx_product_created_at ON product (created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_barcode_tenant
    ON product (barcode, tenant_id) WHERE barcode IS NOT NULL;

-- inventory
CREATE INDEX IF NOT EXISTS idx_inv_tenant_id  ON inventory (tenant_id);
CREATE INDEX IF NOT EXISTS idx_inv_status     ON inventory (status);
CREATE INDEX IF NOT EXISTS idx_inv_deleted    ON inventory (deleted);
CREATE INDEX IF NOT EXISTS idx_inv_low_stock  ON inventory (quantity_in_stock, reorder_level, deleted);
CREATE INDEX IF NOT EXISTS idx_inv_expiry     ON inventory (expiry_date, deleted);

-- inventory_movement
CREATE INDEX IF NOT EXISTS idx_im_inventory_id ON inventory_movement (inventory_id);
CREATE INDEX IF NOT EXISTS idx_im_tenant_id    ON inventory_movement (tenant_id);
CREATE INDEX IF NOT EXISTS idx_im_type         ON inventory_movement (movement_type);
CREATE INDEX IF NOT EXISTS idx_im_created_at   ON inventory_movement (created_at);

-- customers
CREATE INDEX IF NOT EXISTS idx_cust_tenant_id ON customers (tenant_id);
CREATE INDEX IF NOT EXISTS idx_cust_deleted   ON customers (deleted);

-- orders
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id   ON orders (tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_deleted_at  ON orders (deleted_at);
CREATE INDEX IF NOT EXISTS idx_orders_order_type  ON orders (order_type);

-- order_items
CREATE INDEX IF NOT EXISTS idx_oi_tenant_id    ON order_items (tenant_id);
CREATE INDEX IF NOT EXISTS idx_oi_order_id     ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_oi_status       ON order_items (status);
CREATE INDEX IF NOT EXISTS idx_oi_employee     ON order_items (assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_oi_item_type    ON order_items (item_type);

-- invoices
CREATE INDEX IF NOT EXISTS idx_inv_tenant_id   ON invoices (tenant_id);
CREATE INDEX IF NOT EXISTS idx_inv_order_id    ON invoices (order_id);
CREATE INDEX IF NOT EXISTS idx_inv_status      ON invoices (status);
CREATE INDEX IF NOT EXISTS idx_inv_deleted     ON invoices (deleted);
CREATE INDEX IF NOT EXISTS idx_inv_direction   ON invoices (tenant_id, direction);

CREATE INDEX IF NOT EXISTS idx_ci_item_type    ON cart_items (item_type);

-- carts
CREATE INDEX IF NOT EXISTS idx_carts_tenant_id   ON carts (tenant_id);
CREATE INDEX IF NOT EXISTS idx_carts_customer_id ON carts (customer_id);
CREATE INDEX IF NOT EXISTS idx_carts_status      ON carts (status);
CREATE INDEX IF NOT EXISTS idx_carts_created_at  ON carts (created_at);

-- promotions
CREATE INDEX IF NOT EXISTS idx_promo_tenant_id ON promotions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_promo_deleted   ON promotions (deleted);

-- loyalty_transactions
CREATE INDEX IF NOT EXISTS idx_lt_tenant_id   ON loyalty_transactions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_lt_customer_id ON loyalty_transactions (customer_id);
CREATE INDEX IF NOT EXISTS idx_lt_order_id    ON loyalty_transactions (order_id);

-- employees
CREATE INDEX IF NOT EXISTS idx_emp_tenant_id ON employees (tenant_id);
CREATE INDEX IF NOT EXISTS idx_emp_active    ON employees (active);
CREATE INDEX IF NOT EXISTS idx_emp_deleted   ON employees (deleted);

-- purchase_orders
CREATE INDEX IF NOT EXISTS idx_po_tenant_id ON purchase_orders (tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_vendor_id ON purchase_orders (vendor_id);
CREATE INDEX IF NOT EXISTS idx_po_status    ON purchase_orders (status);

-- market_prices
CREATE INDEX IF NOT EXISTS idx_mp_tenant_id ON market_prices (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mp_active    ON market_prices (is_active);

-- buyback_orders
CREATE INDEX IF NOT EXISTS idx_bb_tenant_id   ON buyback_orders (tenant_id);
CREATE INDEX IF NOT EXISTS idx_bb_status      ON buyback_orders (status);
CREATE INDEX IF NOT EXISTS idx_bb_customer_id ON buyback_orders (customer_id);

-- pawn
CREATE INDEX IF NOT EXISTS idx_pawn_tenant_id   ON pawn (tenant_id);
CREATE INDEX IF NOT EXISTS idx_pawn_customer_id ON pawn (customer_id);
CREATE INDEX IF NOT EXISTS idx_pawn_status      ON pawn (status);
CREATE INDEX IF NOT EXISTS idx_pawn_category    ON pawn (tenant_id, pawn_category);

-- legacy_id migration-support indexes (partial — only rows where legacy_id is populated)
CREATE INDEX IF NOT EXISTS idx_product_legacy_id        ON product        (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pawn_legacy_id           ON pawn           (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pawn_req_money_legacy_id ON pawn_req_money (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customers_legacy_id      ON customers      (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_employees_legacy_id      ON employees      (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orders_legacy_id         ON orders         (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

-- pawn typed detail tables
CREATE INDEX IF NOT EXISTS idx_pie_tenant_pawn   ON pawn_item_electronics (tenant_id, pawn_id);
CREATE INDEX IF NOT EXISTS idx_pie_brand         ON pawn_item_electronics (tenant_id, brand);
CREATE INDEX IF NOT EXISTS idx_pie_imei          ON pawn_item_electronics (tenant_id, imei);
CREATE INDEX IF NOT EXISTS idx_piv_tenant_pawn   ON pawn_item_vehicle     (tenant_id, pawn_id);
CREATE INDEX IF NOT EXISTS idx_piv_brand         ON pawn_item_vehicle     (tenant_id, brand);
CREATE INDEX IF NOT EXISTS idx_piv_license_plate ON pawn_item_vehicle     (tenant_id, license_plate);
CREATE INDEX IF NOT EXISTS idx_piw_tenant_pawn   ON pawn_item_watch       (tenant_id, pawn_id);
CREATE INDEX IF NOT EXISTS idx_piw_brand         ON pawn_item_watch       (tenant_id, brand);
CREATE INDEX IF NOT EXISTS idx_pire_tenant_pawn  ON pawn_item_real_estate (tenant_id, pawn_id);
CREATE INDEX IF NOT EXISTS idx_pire_cert         ON pawn_item_real_estate (tenant_id, certificate_number);
CREATE INDEX IF NOT EXISTS idx_pig_tenant_pawn   ON pawn_item_general     (tenant_id, pawn_id);

-- shop_expense
CREATE INDEX IF NOT EXISTS idx_expense_tenant_id ON shop_expense (tenant_id);
CREATE INDEX IF NOT EXISTS idx_expense_date      ON shop_expense (expense_date);
CREATE INDEX IF NOT EXISTS idx_expense_category  ON shop_expense (category);

-- api_audit_log
CREATE INDEX IF NOT EXISTS idx_aal_tenant_id   ON api_audit_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_aal_trace_id    ON api_audit_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_aal_endpoint    ON api_audit_log (api_endpoint);
CREATE INDEX IF NOT EXISTS idx_aal_created_at  ON api_audit_log (created_at);

-- gold_price
CREATE INDEX IF NOT EXISTS idx_gp_tenant_id ON gold_price (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_gold_price_category_tenant
    ON gold_price (category_id, tenant_id)
    WHERE category_id IS NOT NULL;

-- contact_leads
CREATE INDEX IF NOT EXISTS idx_contact_leads_created_at ON contact_leads (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contact_leads_status     ON contact_leads (status);

-- product_catalog
CREATE INDEX IF NOT EXISTS idx_product_catalog_barcode ON product_catalog (barcode);

-- ════════════════════════════════════════════════════════════
-- SECTION 6: updated_at triggers
-- ════════════════════════════════════════════════════════════

DO $$
DECLARE t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'agents','banks',
    'features','roles','users','role_features','notifications','notification_preferences',
    'product_type','attribute_group','attribute_definition','category',
    'vendors','product','product_attribute_value','variant_types','variant_type_options',
    'inventory','inventory_movement','customers',
    'orders','order_items','invoices','invoice_items',
    'carts','cart_items','promotions',
    'loyalty_programs','loyalty_tiers','loyalty_transactions',
    'employees','purchase_orders','purchase_order_items',
    'market_prices','buyback_orders','buyback_order_items',
    'shop_info','shop_config','print_templates','bank_accounts','shop_expense',
    'gold_price','jewelry_counters','user_feedback',
    'contact_leads','product_catalog'
    -- pawn typed detail tables have no updated_at — excluded intentionally
  ]
  LOOP
    EXECUTE format(
      'CREATE OR REPLACE TRIGGER trg_set_updated_at
       BEFORE UPDATE ON %I
       FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
      t
    );
  END LOOP;
END;
$$;

-- ════════════════════════════════════════════════════════════
-- SECTION 7: Row Level Security
--
-- Three-layer tenant isolation:
--   Layer 1: TenantContext (ThreadLocal)          — app layer
--   Layer 2: Hibernate @Filter                    — ORM layer
--   Layer 3: PostgreSQL RLS (this section)        — DB layer
--
-- Session variable: set_config('app.current_tenant', tenantId, true)
--   Empty string / missing → master context (sees NULL tenant_id rows)
--   Non-empty              → tenant context (sees own rows only)
--
-- IMPORTANT: Flyway must connect as a PostgreSQL superuser (bypasses RLS).
-- The application connects as a non-superuser role subject to these policies.
-- ════════════════════════════════════════════════════════════

-- Helper: returns current tenant id or NULL for master context
CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS TEXT LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('app.current_tenant', true), '')
$$;

-- ── Unified tables: NULL tenant_id = master, value = tenant ───
-- IS NOT DISTINCT FROM handles NULL equality correctly:
--   master context (NULL) sees rows where tenant_id IS NULL
--   tenant context ('x')  sees rows where tenant_id = 'x'
--
-- features, user_roles, role_features, refresh_tokens have NO RLS:
--   features — global platform data, no tenant_id column.
--   user_roles/role_features — tenant isolation enforced via FK to users/roles.
--   refresh_tokens — no tenant_id column.

ALTER TABLE roles           ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles           FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON roles
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE users           FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE active_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE active_sessions FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON active_sessions
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE notifications   ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications   FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notifications
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notification_preferences
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE activity_log    ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_log    FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON activity_log
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

-- ── Tenant-only tables: tenant_id must match exactly ──────────

DO $$
DECLARE t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'product_type','attribute_group','attribute_definition',
    'category','vendors','product','product_category',
    'product_attribute_value','variant_types','variant_type_options',
    'inventory','inventory_movement',
    'customers','invoice_buyers',
    'orders','order_items','invoices','invoice_items',
    'carts','cart_items','promotions',
    'loyalty_programs','loyalty_tiers','loyalty_transactions',
    'employees',
    'purchase_orders','purchase_order_items',
    'market_prices','buyback_orders','buyback_order_items',
    'pawn','pawn_audit','pawn_req_money','pawn_req_money_audit',
    'pawn_item_electronics','pawn_item_vehicle','pawn_item_watch',
    'pawn_item_real_estate','pawn_item_general',
    'shop_info','shop_config','print_templates',
    'bank_accounts','shop_expense','api_audit_log',
    'gold_price','jewelry_counters'
  ]
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('ALTER TABLE %I FORCE  ROW LEVEL SECURITY', t);
    EXECUTE format(
      'CREATE POLICY tenant_isolation ON %I USING (tenant_id = current_tenant_id())',
      t
    );
  END LOOP;
END;
$$;

-- ── Master-only tables: no RLS needed ─────────────────────────
-- tenants, agents, user_feedback are only ever accessed
-- in master context (no tenant session variable set). No policies added.

-- ── Global reference tables: no RLS needed ────────────────────
-- banks is a read-only global reference table accessible from any context.

-- ════════════════════════════════════════════════════════════
-- SECTION 8: Master seed data
-- All INSERT statements use ON CONFLICT DO NOTHING — safe to re-run.
-- tenant_id = NULL on all rows (master-scope records).
-- ════════════════════════════════════════════════════════════

-- ── 1. Platform features ──────────────────────────────────────
INSERT INTO features (id, name, display_name, description, active, deleted)
VALUES
    -- Shop management
    (202601001, 'DASHBOARD',        'Bảng Điều Khiển',          'Xem tổng quan và thống kê chính của cửa hàng',                   TRUE, FALSE),
    (202601002, 'ORDER',            'Đơn Hàng',                  'Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',      TRUE, FALSE),
    (202601003, 'MY_WORK',          'Công Việc Của Tôi',         'Xem công việc được giao cho nhân viên hiện tại',                  TRUE, FALSE),
    (202601004, 'PRODUCT',          'Sản Phẩm & Dịch Vụ',       'Quản lý danh sách sản phẩm, dịch vụ, giá cả',                    TRUE, FALSE),
    (202601005, 'PROMOTION',        'Khuyến Mãi',                'Tạo và quản lý các chương trình khuyến mãi, giảm giá',            TRUE, FALSE),
    (202601006, 'EMPLOYEE',         'Nhân Viên',                 'Quản lý nhân viên, chức vụ, lương cơ bản',                       TRUE, FALSE),
    (202601007, 'SALARY',           'Lương Nhân Viên',           'Quản lý bảng lương, tính toán lương, chi trả',                   TRUE, FALSE),
    (202601008, 'CUSTOMER',         'Khách Hàng',                'Quản lý thông tin khách hàng, lịch sử mua hàng, tích điểm',      TRUE, FALSE),
    (202601009, 'INVOICE',          'Hóa Đơn',                   'Quản lý hóa đơn, xuất hóa đơn điện tử',                         TRUE, FALSE),
    (202601010, 'REVENUE',          'Doanh Thu',                 'Xem báo cáo doanh thu, lợi nhuận, chi phí',                      TRUE, FALSE),
    (202601011, 'USER',             'Người Dùng',                'Quản lý tài khoản người dùng, quyền truy cập',                   TRUE, FALSE),
    (202601012, 'SHOP_INFO',        'Thông Tin Cửa Hàng',        'Cập nhật thông tin cửa hàng, cấu hình hệ thống',                 TRUE, FALSE),
    -- Master / system management
    (202601013, 'TENANT_MGMT',      'Quản Lý Cửa Hàng',         'Tạo, kích hoạt và quản lý các cửa hàng trong hệ thống',          TRUE, FALSE),
    -- Supply chain
    (202601014, 'VENDOR',           'Nhà Cung Cấp',              'Quản lý nhà cung cấp và đơn đặt hàng nhập',                      TRUE, FALSE),
    (202601015, 'AGENT_MGMT',       'Đại Lý',                    'Super admin quản lý đại lý và giao shop',                        TRUE, FALSE),
    -- Operations
    (202601016, 'INVENTORY',        'Quản Lý Kho',               'Quản lý tồn kho, nhập xuất kho và kiểm kho',                     TRUE, FALSE),
    (202601017, 'POS',              'Điểm Bán Hàng',             'Bán hàng tại quầy, thanh toán và in hóa đơn',                    TRUE, FALSE),
    (202601018, 'ACTIVITY_LOG',     'Nhật Ký Hoạt Động',         'Xem nhật ký hoạt động của người dùng trong cửa hàng',            TRUE, FALSE),
    (202601019, 'PAWN',             'Cầm Đồ',                    'Quản lý hợp đồng cầm đồ, lãi suất và thanh lý tài sản',          TRUE, FALSE),
    (202601020, 'FEEDBACK_MGMT',    'Quản Lý Phản Hồi',          'Xem và xử lý phản hồi, góp ý từ người dùng toàn hệ thống',       TRUE, FALSE),
    (202601021, 'MASTER_DASHBOARD', 'Bảng Điều Khiển Hệ Thống', 'Xem tổng quan và thống kê của hệ thống master',                  TRUE, FALSE),
    (202601022, 'LOYALTY',          'Tích Điểm Khách Hàng',      'Chương trình tích điểm và phần thưởng khách hàng',               TRUE, FALSE),
    (202601023, 'EXPENSE',          'Chi Phí',                   'Theo dõi và quản lý chi phí hoạt động cửa hàng',                 TRUE, FALSE),
    (202601024, 'NOTIFICATION',     'Thông Báo',                 'Nhận thông báo và nhắc nhở từ hệ thống',                         TRUE, FALSE),
    (202601025, 'FEEDBACK',         'Góp Ý',                     'Gửi phản hồi và đề xuất đến quản trị hệ thống',                  TRUE, FALSE),
    (202601026, 'PRINT_TEMPLATE',   'Mẫu In',                    'Quản lý mẫu in biên nhận và hóa đơn',                            TRUE, FALSE),
    (202601027, 'BANK_ACCOUNT',     'Tài Khoản Ngân Hàng',       'Quản lý tài khoản ngân hàng của cửa hàng',                       TRUE, FALSE),
    (202601028, 'ACCOUNTING',       'Kế Toán',                   'Xem báo cáo kế toán tổng hợp',                                   TRUE, FALSE),
    -- Master-only management features
    (202601029, 'CONTACT_LEAD_MGMT','Đăng Ký Dùng Thử',          'Xem và quản lý các yêu cầu đăng ký dùng thử từ trang chủ',       TRUE, FALSE),
    (202601030, 'PRODUCT_CATALOG',  'Danh Mục Sản Phẩm',         'Quản lý danh mục sản phẩm dùng chung toàn hệ thống',             TRUE, FALSE),
    -- Sub-feature: granular order visibility within the ORDER module
    (202601031, 'ORDER_VIEW_ALL',   'Xem Tất Cả Đơn Hàng',       'Xem đơn hàng của tất cả nhân viên; nếu không có quyền này, chỉ xem được đơn hàng tự tạo', TRUE, FALSE),
    -- Gold price management (used by jewelry shops and pawn shops)
    (202601033, 'GOLD_PRICE',       'Bảng Giá Vàng',              'Quản lý bảng giá vàng theo tuổi, dùng cho tính giá mua/bán và cầm đồ',                    TRUE, FALSE),
    (202601034, 'GOLD_PRICE_CHART', 'Biểu Đồ Giá Vàng',           'Xem biểu đồ giá vàng thế giới (XAU/USD) theo thời gian thực',                              TRUE, FALSE),
    -- Commission / salary sub-features
    (202601035, 'COMMISSION',       'Hoa Hồng Nhân Viên',          'Gán nhân viên thực hiện và tính hoa hồng cho từng sản phẩm/dịch vụ trong đơn hàng',        TRUE, FALSE),
    (202601036, 'SALARY_VIEW_ALL',  'Xem Tất Cả Bảng Lương',       'Xem bảng lương của tất cả nhân viên; nếu không có quyền này, chỉ xem được bảng lương của bản thân', TRUE, FALSE),
    -- Integrations
    (202601037, 'GOOGLE_DRIVE',     'Tích Hợp Google Drive',       'Kết nối Google Drive cá nhân để lưu ảnh sản phẩm, hình căn cước khách hàng và ảnh hợp đồng cầm đồ', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('features', 'id'), 202601037, true);

-- ── 2. Master roles ───────────────────────────────────────────
INSERT INTO roles (id, tenant_id, name, description, deleted)
VALUES
    (202600001, NULL, 'MASTER_TENANT', 'Quản trị hệ thống - Toàn quyền quản lý tenant và người dùng master', FALSE),
    (202600002, NULL, 'AGENT',              'Quản trị đại lý - Quản lý danh sách shop thuộc đại lý', FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('roles', 'id'), 202600002, true);

-- ── 3. Role-feature mappings (master roles) ───────────────────
INSERT INTO role_features (role_id, feature_id)
VALUES
    -- MASTER_TENANT: full master feature set
    (202600001, 202601011),   -- USER
    (202600001, 202601013),   -- TENANT_MGMT
    (202600001, 202601015),   -- AGENT_MGMT
    (202600001, 202601018),   -- ACTIVITY_LOG
    (202600001, 202601020),   -- FEEDBACK_MGMT
    (202600001, 202601021),   -- MASTER_DASHBOARD
    (202600001, 202601024),   -- NOTIFICATION
    (202600001, 202601029),   -- CONTACT_LEAD_MGMT
    (202600001, 202601030),   -- PRODUCT_CATALOG
    -- AGENT: tenant management + master dashboard + notifications
    (202600002, 202601013),   -- TENANT_MGMT
    (202600002, 202601021),   -- MASTER_DASHBOARD
    (202600002, 202601024)    -- NOTIFICATION
ON CONFLICT (role_id, feature_id) DO NOTHING;

-- ── 4. Default admin user ─────────────────────────────────────
-- Password = '1234' (bcrypt cost 10). CHANGE on first login.
INSERT INTO users (id, tenant_id, username, email, password, full_name,
                   active, account_non_locked, credentials_non_expired, account_non_expired,
                   failed_login_attempts, lang, deleted)
VALUES (79260001, NULL, 'Administrator', 'nguyendangkhoa25@gmail.com',
        '$2a$10$u2vWhVe3r0JliVDtwrkU4eqEBUWmXPsZ4dyazsbuoco5gL6L2N8B.',
        'Quản Trị Viên', TRUE, TRUE, TRUE, TRUE, 0, 'vi', FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), 79260001, true);

INSERT INTO user_roles (user_id, role_id)
VALUES (79260001, 202600001)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ── 5. Vietnamese bank list (VietQR BIN reference) ───────────────
-- Source: VietQR API — all licensed banks in Vietnam as of 2025.
INSERT INTO banks (code, bin, name, short_name, sort_order) VALUES
-- ── Big 4 state-owned / majority state-owned ──────────────────────────────────────────────
('VCB',      '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam',                              'Vietcombank',           1),
('CTG',      '970415', 'Ngân hàng TMCP Công thương Việt Nam',                               'VietinBank',            2),
('BID',      '970418', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam',                      'BIDV',                  3),
('AGR',      '970405', 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam',            'Agribank',              4),
-- ── Top private joint-stock banks ────────────────────────────────────────────────────────
('MBB',      '970422', 'Ngân hàng TMCP Quân đội',                                           'MB Bank',               5),
('TCB',      '970407', 'Ngân hàng TMCP Kỹ thương Việt Nam',                                 'Techcombank',           6),
('VPB',      '970432', 'Ngân hàng TMCP Việt Nam Thịnh Vượng',                               'VPBank',                7),
('ACB',      '970416', 'Ngân hàng TMCP Á Châu',                                             'ACB',                   8),
('STB',      '970403', 'Ngân hàng TMCP Sài Gòn Thương Tín',                                 'Sacombank',             9),
('TPB',      '970423', 'Ngân hàng TMCP Tiên Phong',                                         'TPBank',               10),
('HDB',      '970437', 'Ngân hàng TMCP Phát triển TP.HCM',                                  'HDBank',               11),
('VIB',      '970441', 'Ngân hàng TMCP Quốc tế Việt Nam',                                   'VIB',                  12),
('SHB',      '970443', 'Ngân hàng TMCP Sài Gòn - Hà Nội',                                  'SHB',                  13),
('EIB',      '970431', 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam',                            'Eximbank',             14),
('LPB',      '970449', 'Ngân hàng TMCP Bưu điện Liên Việt',                                 'LienVietPostBank',     15),
('MSB',      '970426', 'Ngân hàng TMCP Hàng Hải Việt Nam',                                  'MSB',                  16),
('OCB',      '970448', 'Ngân hàng TMCP Phương Đông',                                        'OCB',                  17),
('SSB',      '970440', 'Ngân hàng TMCP Đông Nam Á',                                         'SeABank',              18),
('ABB',      '970425', 'Ngân hàng TMCP An Bình',                                             'ABBank',               19),
('BAB',      '970409', 'Ngân hàng TMCP Bắc Á',                                              'BacABank',             20),
('BVB',      '970454', 'Ngân hàng TMCP Bản Việt',                                           'BVBank',               21),
('KLB',      '970462', 'Ngân hàng TMCP Kiên Long',                                          'KienLongBank',         22),
('NAB',      '970428', 'Ngân hàng TMCP Nam Á',                                              'NamABank',             23),
('NCB',      '970419', 'Ngân hàng TMCP Quốc Dân',                                           'NCB',                  24),
('PGB',      '970430', 'Ngân hàng TMCP Xăng dầu Petrolimex',                                'PGBank',               25),
('PVCB',     '970452', 'Ngân hàng TMCP Đại Chúng Việt Nam',                                 'PVcomBank',            26),
('DAB',      '970406', 'Ngân hàng TMCP Đông Á',                                             'DongABank',            27),
('VAB',      '970427', 'Ngân hàng TMCP Việt Á',                                             'VietABank',            28),
('VBB',      '970433', 'Ngân hàng TMCP Việt Nam Thương Tín',                                'VietBank',             29),
('SGCB',     '970400', 'Ngân hàng TMCP Sài Gòn Công Thương',                                'SaigonBank',           30),
('BAOVIET',  '970438', 'Ngân hàng TMCP Bảo Việt',                                           'BaoViet Bank',         31),
('SCB',      '970429', 'Ngân hàng TMCP Sài Gòn',                                            'SCB',                  32),
-- ── Policy / cooperative banks ───────────────────────────────────────────────────────────
('COOPBANK', '970446', 'Ngân hàng Hợp tác xã Việt Nam',                                     'Co-opBank',            35),
('VBSP',     '999888', 'Ngân hàng Chính sách xã hội Việt Nam',                              'VBSP',                 36),
('VDB',      '006',    'Ngân hàng Phát triển Việt Nam',                                     'VDB',                  37),
-- ── Banks under special control ─────────────────────────────────────────────────────────
('OJB',      '970414', 'Ngân hàng TMCP Đại Dương',                                          'OceanBank',            38),
('CBB',      '970444', 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam',                   'CBBank',               39),
('GPB',      '970408', 'Ngân hàng TMCP Dầu khí Toàn cầu',                                   'GPBank',               40),
-- ── Digital / neobanks ───────────────────────────────────────────────────────────────────
('CAKE',     '546034', 'CAKE by VPBank',                                                     'CAKE',                 45),
('UBANK',    '546035', 'Ubank by VPBank',                                                    'Ubank',                46),
-- ── Foreign bank branches & JV banks ────────────────────────────────────────────────────
('HSBC',     '458761', 'Ngân hàng TNHH MTV HSBC Việt Nam',                                  'HSBC Vietnam',         50),
('SC',       '970410', 'Ngân hàng TNHH MTV Standard Chartered Việt Nam',                    'Standard Chartered',   51),
('SHIN',     '970424', 'Ngân hàng TNHH MTV Shinhan Việt Nam',                               'Shinhan Vietnam',      52),
('WOORI',    '970457', 'Ngân hàng TNHH MTV Woori Việt Nam',                                 'Woori Vietnam',        53),
('UOB',      '970458', 'Ngân hàng UOB Việt Nam',                                            'UOB Vietnam',          54),
('CIMB',     '422589', 'Ngân hàng TNHH MTV CIMB Việt Nam',                                  'CIMB Vietnam',         55),
('PBVN',     '970439', 'Ngân hàng TNHH MTV Public Bank Việt Nam',                           'PublicBank Vietnam',   56),
('IVB',      '970434', 'Ngân hàng TNHH Indochina',                                          'Indovina Bank',        57),
('ANZ',      '970421', 'Ngân hàng ANZ Việt Nam',                                            'ANZ Vietnam',          58)
ON CONFLICT (code) DO NOTHING;

-- ── 6. Backfill shop-type-specific print templates for existing tenants ──────
-- On a fresh install there are no tenants yet, so these are no-ops.
-- They exist so re-running V001 against an existing deployment is safe.
SET row_security = off;

INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default, deleted, created_at, updated_at)
SELECT t.tenant_id, 'POS_RECEIPT', 'Phiếu dịch vụ',
    '{"headerText":"","footerText":"Cảm ơn quý khách!\nHẹn gặp lại!","showAddress":true,"showTaxId":false,"showOrderNumber":true,"showDateTime":true,"showCustomer":true,"showTaxBreakdown":false,"showCashDetails":true,"paperWidth":"80mm","autoClose":true,"showVietQr":false}',
    FALSE, FALSE, NOW(), NOW()
FROM tenants t WHERE t.shop_type IN ('BARBER_SHOP', 'COFFEE_SHOP', 'FOOD_BEVERAGE', 'RESTAURANT')
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default, deleted, created_at, updated_at)
SELECT t.tenant_id, 'POS_RECEIPT', 'Hóa đơn thuốc',
    '{"headerText":"","footerText":"Cảm ơn quý khách!\nChúc bạn mau hồi phục!","showAddress":true,"showTaxId":true,"showOrderNumber":true,"showDateTime":true,"showCustomer":true,"showTaxBreakdown":true,"showCashDetails":true,"paperWidth":"80mm","autoClose":true,"showVietQr":false}',
    FALSE, FALSE, NOW(), NOW()
FROM tenants t WHERE t.shop_type = 'PHARMACY'
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default, deleted, created_at, updated_at)
SELECT t.tenant_id, 'POS_RECEIPT', 'Hóa đơn siêu thị',
    '{"headerText":"","footerText":"Cảm ơn quý khách!\nHẹn gặp lại!","showAddress":true,"showTaxId":false,"showOrderNumber":true,"showDateTime":true,"showCustomer":false,"showTaxBreakdown":false,"showCashDetails":true,"paperWidth":"80mm","autoClose":true,"showVietQr":true}',
    FALSE, FALSE, NOW(), NOW()
FROM tenants t WHERE t.shop_type = 'CONVENIENCE_STORE'
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default, deleted, created_at, updated_at)
SELECT t.tenant_id, 'POS_RECEIPT', 'Phiếu bảo hành',
    '{"headerText":"","footerText":"Cảm ơn quý khách!\nVui lòng giữ hóa đơn để bảo hành.","showAddress":true,"showTaxId":true,"showOrderNumber":true,"showDateTime":true,"showCustomer":true,"showTaxBreakdown":true,"showCashDetails":true,"paperWidth":"80mm","autoClose":true,"showVietQr":false}',
    FALSE, FALSE, NOW(), NOW()
FROM tenants t WHERE t.shop_type IN ('FASHION', 'ELECTRONICS')
ON CONFLICT (template_type, name, tenant_id) DO NOTHING;

SET row_security = on;

-- ── 6b. Backfill default POS_RECEIPT print template for existing active tenants ──
-- On a fresh install there are no tenants yet, so this loop is a no-op.
-- It exists so the migration is safe to apply against a database that was
-- previously bootstrapped without print_template seeding in the DML scripts.
DO $$
DECLARE
    v_tenant_id TEXT;
    v_config    TEXT := '{"headerText":"","footerText":"Cảm ơn quý khách!\nHẹn gặp lại!","showAddress":true,"showTaxId":false,"showOrderNumber":true,"showDateTime":true,"showCustomer":true,"showTaxBreakdown":false,"showCashDetails":true,"paperWidth":"80mm","autoClose":true,"showVietQr":false}';
BEGIN
    FOR v_tenant_id IN
        SELECT tenant_id FROM tenants WHERE active = TRUE
    LOOP
        PERFORM set_config('app.current_tenant', v_tenant_id, true);
        IF NOT EXISTS (
            SELECT 1 FROM print_templates
            WHERE template_type = 'POS_RECEIPT' AND deleted = FALSE
        ) THEN
            INSERT INTO print_templates (tenant_id, template_type, name, config_json, is_default)
            VALUES (v_tenant_id, 'POS_RECEIPT', 'Mặc định', v_config, TRUE)
            ON CONFLICT DO NOTHING;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_tenant', '', true);
END $$;

-- ── 7. Product catalog seed data (common Vietnamese consumer products) ──────────
INSERT INTO product_catalog (barcode, name, brand, category_hint, unit, description, source) VALUES
    ('8934563143326', 'Mì Hảo Hảo Tôm Chua Cay 75g',           'Acecook',       'Thực phẩm',        'Gói',  'Mì ăn liền tôm chua cay',                    'SEED'),
    ('8934563141841', 'Mì Hảo Hảo Sườn Heo Xào 75g',           'Acecook',       'Thực phẩm',        'Gói',  'Mì ăn liền sườn heo xào',                    'SEED'),
    ('8936014130013', 'Nước Tương Maggi 200ml',                  'Nestlé',        'Thực phẩm',        'Chai', 'Nước tương cao cấp Maggi 200ml',              'SEED'),
    ('8934588032506', 'Nước Mắm Phú Quốc 40 độ đạm 500ml',     'Chinsu',        'Thực phẩm',        'Chai', 'Nước mắm Phú Quốc truyền thống',             'SEED'),
    ('8935049500017', 'Dầu Ăn Neptune 1L',                       'Neptune',       'Thực phẩm',        'Chai', 'Dầu ăn cao cấp Neptune 1 lít',               'SEED'),
    ('8934868203218', 'Gạo ST25 5kg',                            'Hồ Quang Trí', 'Thực phẩm',        'Túi',  'Gạo ST25 thơm ngon 5kg',                     'SEED'),
    ('8936048770012', 'Nước Rửa Chén Sunlight Chanh 750ml',     'Unilever',      'Hóa phẩm',         'Chai', 'Nước rửa chén Sunlight hương chanh',          'SEED'),
    ('8934588014014', 'Bột Giặt Omo 800g',                       'Unilever',      'Hóa phẩm',         'Hộp',  'Bột giặt Omo tẩy sạch cực mạnh',             'SEED'),
    ('8934564278093', 'Kem Đánh Răng Colgate 225g',             'Colgate',       'Chăm sóc cá nhân', 'Hộp',  'Kem đánh răng Colgate bảo vệ 12 giờ',        'SEED'),
    ('8935049040027', 'Dầu Gội Clear Men 170ml',                 'Unilever',      'Chăm sóc cá nhân', 'Chai', 'Dầu gội Clear Men sạch gàu',                 'SEED'),
    ('8934868015016', 'Nước Uống Aquafina 500ml',                'PepsiCo',       'Đồ uống',          'Chai', 'Nước uống tinh khiết Aquafina 500ml',         'SEED'),
    ('8934868015023', 'Nước Uống Aquafina 1.5L',                 'PepsiCo',       'Đồ uống',          'Chai', 'Nước uống tinh khiết Aquafina 1.5L',          'SEED'),
    ('8934822400038', 'Coca-Cola 330ml (lon)',                    'Coca-Cola',     'Đồ uống',          'Lon',  'Nước ngọt Coca-Cola lon 330ml',               'SEED'),
    ('8934822400052', 'Pepsi 330ml (lon)',                        'PepsiCo',       'Đồ uống',          'Lon',  'Nước ngọt Pepsi lon 330ml',                   'SEED'),
    ('8934822400069', '7UP 330ml (lon)',                          'PepsiCo',       'Đồ uống',          'Lon',  'Nước ngọt 7UP lon 330ml',                     'SEED'),
    ('8936012640029', 'Trà Xanh Không Độ 500ml',                'THP',           'Đồ uống',          'Chai', 'Trà xanh Không Độ 500ml',                     'SEED'),
    ('8936012640036', 'Nước Tăng Lực Number 1 330ml',           'THP',           'Đồ uống',          'Lon',  'Nước tăng lực Number 1 lon 330ml',            'SEED'),
    ('8934822400076', 'Red Bull 250ml (lon)',                     'Red Bull',      'Đồ uống',          'Lon',  'Nước tăng lực Red Bull lon 250ml',            'SEED'),
    ('8935232200028', 'Sữa Tươi Vinamilk Full Cream 1L',        'Vinamilk',      'Đồ uống',          'Hộp',  'Sữa tươi tiệt trùng Vinamilk toàn phần 1L',  'SEED'),
    ('8935232200035', 'Sữa Tươi TH True Milk 1L',               'TH',            'Đồ uống',          'Hộp',  'Sữa tươi sạch TH True Milk 1L',              'SEED'),
    ('8935049890016', 'Bánh Oreo Chocolate 97g',                 'Mondelez',      'Thực phẩm',        'Gói',  'Bánh quy nhân kem chocolate Oreo',            'SEED'),
    ('8934563200020', 'Snack Oishi Tôm 40g',                    'Oishi',         'Thực phẩm',        'Gói',  'Snack tôm Oishi giòn tan',                    'SEED'),
    ('8936120280019', 'Kẹo Mentos Bạc Hà 37.5g',               'Mentos',        'Thực phẩm',        'Cuộn', 'Kẹo Mentos hương bạc hà',                    'SEED'),
    ('8934588020138', 'Nước Mắm Nam Ngư 500ml',                 'Masan',         'Thực phẩm',        'Chai', 'Nước mắm đặc biệt Nam Ngư 500ml',            'SEED'),
    ('8934588020015', 'Tương Ớt Chinsu 250g',                   'Chinsu',        'Thực phẩm',        'Chai', 'Tương ớt Chinsu đặc biệt',                   'SEED'),
    ('8934822400014', 'Bia Tiger 330ml (lon)',                    'Heineken',      'Đồ uống',          'Lon',  'Bia Tiger lon 330ml',                         'SEED'),
    ('8934822400021', 'Bia Heineken 330ml (lon)',                 'Heineken',      'Đồ uống',          'Lon',  'Bia Heineken lon 330ml',                      'SEED'),
    ('8934822400045', 'Bia Saigon Đỏ 333ml (lon)',              'Sabeco',        'Đồ uống',          'Lon',  'Bia Saigon Đỏ lon 333ml',                    'SEED'),
    ('8934868202099', 'Cà Phê Trung Nguyên 1 500g',             'Trung Nguyên', 'Đồ uống',          'Gói',  'Cà phê rang xay Trung Nguyên loại 1',         'SEED'),
    ('8936014130006', 'Cà Phê Nescafé Classic 200g',            'Nestlé',        'Đồ uống',          'Hộp',  'Cà phê hoà tan Nescafé Classic',              'SEED')
ON CONFLICT (barcode) DO NOTHING;

-- ════════════════════════════════════════════════════════════
-- SECTION 9: Schema cleanup
-- Removed redundant jewelry EAV attributes (gold type and brand
-- are captured by category and vendor; silver derived from category).
-- Safe no-op on fresh installs where these were never seeded.
-- ════════════════════════════════════════════════════════════

DELETE FROM product_attribute_value
WHERE attribute_id IN (
    SELECT id FROM attribute_definition
    WHERE code IN ('gold_type_code', 'gold_brand_code', 'is_silver')
);

DELETE FROM attribute_definition
WHERE code IN ('gold_type_code', 'gold_brand_code', 'is_silver');

-- Normalize product unit values from Vietnamese words to English codes.
UPDATE product SET unit = 'piece'  WHERE unit = 'cái';
UPDATE product SET unit = 'can'    WHERE unit = 'lon';
UPDATE product SET unit = 'bottle' WHERE unit = 'chai';
UPDATE product SET unit = 'pack'   WHERE unit = 'gói';
UPDATE product SET unit = 'box'    WHERE unit = 'hộp';
UPDATE product SET unit = 'bag'    WHERE unit = 'bao';
UPDATE product SET unit = 'tube'   WHERE unit = 'tuýp';
UPDATE product SET unit = 'bar'    WHERE unit = 'bánh';
UPDATE product SET unit = 'roll'   WHERE unit = 'cuộn';

-- ════════════════════════════════════════════════════════════
-- Merged from: V002__variant_inventory.sql
-- ════════════════════════════════════════════════════════════

-- V002__variant_inventory.sql
-- Adds per-variant inventory tracking.
--
-- Strategy:
--   • inventory: add variant_id (nullable); replace the product-level unique constraint
--     with two partial unique indexes — one for product-only rows, one for product+variant rows.
--   • cart_items, order_items, purchase_order_items: add variant_id for line-item traceability.
--     No FK constraint on these tables (historical records must survive variant deletion).

-- ─── inventory ───────────────────────────────────────────────────────────────

ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS variant_id BIGINT DEFAULT NULL;

-- FK: block variant deletion when an inventory record references it (stock > 0 guard is in Java).
ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE RESTRICT;

-- Remove the old product-level unique constraint; replaced by the two partial indexes below.
ALTER TABLE inventory
    DROP CONSTRAINT IF EXISTS uq_inventory_product;

-- One inventory record per product when no variant is assigned.
CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_product_no_variant
    ON inventory (product_id)
    WHERE variant_id IS NULL AND deleted = false;

-- One inventory record per (product, variant) combination.
CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_product_variant
    ON inventory (product_id, variant_id)
    WHERE variant_id IS NOT NULL AND deleted = false;

-- Support fast lookup by variant_id (used in CartServiceImpl and PurchaseOrderService).
CREATE INDEX IF NOT EXISTS idx_inventory_variant_id
    ON inventory (variant_id)
    WHERE variant_id IS NOT NULL;

-- ─── cart_items ───────────────────────────────────────────────────────────────

ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS variant_id BIGINT DEFAULT NULL;

-- ─── order_items ─────────────────────────────────────────────────────────────

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS variant_id BIGINT DEFAULT NULL;

-- ─── purchase_order_items ────────────────────────────────────────────────────

ALTER TABLE purchase_order_items
    ADD COLUMN IF NOT EXISTS variant_id BIGINT DEFAULT NULL;

-- ════════════════════════════════════════════════════════════
-- Merged from: V003__mobile_additions.sql
-- ════════════════════════════════════════════════════════════

ALTER TABLE users ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(100);

-- ════════════════════════════════════════════════════════════
-- Merged from: V004__combos.sql
-- ════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS combos (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(20,0) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100)
);
CREATE TABLE IF NOT EXISTS combo_items (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price DECIMAL(20,0) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_combos_tenant ON combos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_combo_items_combo ON combo_items(combo_id);

-- ════════════════════════════════════════════════════════════
-- Merged from: V005__exchange_rates.sql
-- ════════════════════════════════════════════════════════════

-- Master-level exchange rate cache (one row per currency+source, upserted every 30 min)
CREATE TABLE IF NOT EXISTS exchange_rates (
    currency_code VARCHAR(3)   NOT NULL,
    source        VARCHAR(50)  NOT NULL,
    buy_rate      NUMERIC(18, 4),
    transfer_rate NUMERIC(18, 4),
    sell_rate     NUMERIC(18, 4),
    fetched_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (currency_code, source)
);

-- ════════════════════════════════════════════════════════════
-- Merged from: V006__customers_phone_partial_unique.sql
-- ════════════════════════════════════════════════════════════

-- Replace the full unique constraint on (phone, tenant_id) with a partial index
-- that ignores soft-deleted rows, so a new customer can reuse a phone number that
-- belonged to a previously deleted customer.
ALTER TABLE customers DROP CONSTRAINT IF EXISTS uq_customers_phone_tenant;
DROP INDEX IF EXISTS uq_customers_phone_tenant;
CREATE UNIQUE INDEX uq_customers_phone_tenant
    ON customers (phone, tenant_id)
    WHERE deleted = false;

-- ════════════════════════════════════════════════════════════
-- Merged from: V007__product_suggestions.sql
-- ════════════════════════════════════════════════════════════

-- ============================================================
-- V007: Product suggestions for onboarding
-- Global reference table (no tenant_id, no RLS).
-- Products tagged with shop_types[] can appear for multiple
-- shop types; products with dynamic_price=true (e.g. jewelry)
-- are priced at gold market rate, not a fixed default_price.
-- ============================================================

CREATE TABLE IF NOT EXISTS product_suggestions (
    id               BIGSERIAL    PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    emoji            VARCHAR(20),
    default_price    BIGINT       NOT NULL DEFAULT 0,
    unit             VARCHAR(50)  NOT NULL DEFAULT 'Cái',
    product_type_code VARCHAR(50) NOT NULL DEFAULT 'FOOD',
    dynamic_price    BOOLEAN      NOT NULL DEFAULT FALSE,
    shop_types       TEXT[]       NOT NULL DEFAULT '{}',
    display_order    INT          NOT NULL DEFAULT 0,
    category_name    VARCHAR(200) DEFAULT NULL,
    name_en          VARCHAR(200) DEFAULT NULL,
    CONSTRAINT uq_product_suggestion_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_product_suggestions_shop_types
    ON product_suggestions USING GIN(shop_types);

-- ── Seed data ────────────────────────────────────────────────
-- Products shared across multiple shop types come first (lower display_order).
-- Use ON CONFLICT ... DO NOTHING for idempotent re-runs.

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order)
VALUES
-- ── Shared beverages ──────────────────────────────────────────
('Nước suối',              '💧', 5000,    'Chai',  'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT','COFFEE_SHOP'], 1),
('Coca Cola',              '🥤', 12000,   'Lon',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT','COFFEE_SHOP'], 2),
('Pepsi',                  '🥤', 12000,   'Lon',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT','COFFEE_SHOP'], 3),
('Bia Saigon',             '🍺', 15000,   'Lon',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT'], 4),
('Bia Tiger',              '🍺', 16000,   'Lon',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT'], 5),
('Trứng gà',               '🥚', 35000,   'Chục',  'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT','COFFEE_SHOP'], 6),
('Bánh mì',                '🥖', 15000,   'Ổ',     'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','COFFEE_SHOP','RESTAURANT'], 7),

-- ── CONVENIENCE_STORE ─────────────────────────────────────────
('Mì tôm Hảo Hảo',        '🍜', 5000,    'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE'], 10),
('Sữa tươi Vinamilk',      '🥛', 8000,    'Hộp',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE'], 11),
('Dầu ăn',                 '🫒', 45000,   'Chai',  'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE'], 12),
('Gạo',                    '🌾', 25000,   'Kg',    'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE'], 13),
('Nước mắm',               '🫙', 35000,   'Chai',  'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE','RESTAURANT'], 14),
('Đường cát',              '🍬', 22000,   'Kg',    'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE'], 15),
('Bột giặt',               '🧴', 35000,   'Túi',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 16),
('Dầu gội đầu',            '🧴', 55000,   'Chai',  'BEAUTY',   FALSE, ARRAY['CONVENIENCE_STORE'], 17),
('Xà phòng',               '🧼', 15000,   'Bánh',  'BEAUTY',   FALSE, ARRAY['CONVENIENCE_STORE'], 18),
('Khăn giấy',              '🧻', 20000,   'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 19),
('Thuốc lá',               '🚬', 25000,   'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 20),
('Pin AA',                 '🔋', 25000,   'Vỉ',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 21),
('Sạc điện thoại',         '🔌', 150000,  'Cái',   'ELECTRONICS', FALSE, ARRAY['CONVENIENCE_STORE','ELECTRONICS'], 22),
('Cáp USB',                '🔌', 35000,   'Cái',   'ELECTRONICS', FALSE, ARRAY['CONVENIENCE_STORE','ELECTRONICS'], 23),
('Nước tăng lực Redbull',  '🔋', 14000,   'Lon',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE','FOOD_BEVERAGE'], 24),
('Snack khoai tây',        '🍟', 10000,   'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 25),
('Kẹo cao su',             '🍬', 5000,    'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 26),

-- ── FOOD_BEVERAGE ─────────────────────────────────────────────
('Rau cải xanh',           '🥬', 10000,   'Bó',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 30),
('Thịt heo',               '🥩', 120000,  'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 31),
('Thịt bò',                '🥩', 220000,  'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 32),
('Cá tươi',                '🐟', 80000,   'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 33),
('Đậu hũ',                 '🫙', 10000,   'Miếng', 'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 34),
('Cà chua',                '🍅', 20000,   'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 35),
('Hành tây',               '🧅', 15000,   'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 36),
('Tỏi',                    '🧄', 30000,   'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 37),
('Cà rốt',                 '🥕', 18000,   'Kg',    'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 38),
('Dầu hào',                '🫙', 28000,   'Chai',  'FOOD',     FALSE, ARRAY['FOOD_BEVERAGE','RESTAURANT'], 39),

-- ── RESTAURANT ───────────────────────────────────────────────
('Phở bò',                 '🍜', 65000,   'Tô',    'FOOD',     FALSE, ARRAY['RESTAURANT'], 40),
('Cơm tấm sườn',           '🍚', 55000,   'Dĩa',   'FOOD',     FALSE, ARRAY['RESTAURANT'], 41),
('Bún bò Huế',             '🍜', 60000,   'Tô',    'FOOD',     FALSE, ARRAY['RESTAURANT'], 42),
('Bánh mì thịt',           '🥖', 25000,   'Ổ',     'FOOD',     FALSE, ARRAY['RESTAURANT','COFFEE_SHOP'], 43),
('Gỏi cuốn',               '🫔', 35000,   'Phần',  'FOOD',     FALSE, ARRAY['RESTAURANT'], 44),
('Chả giò',                '🧆', 45000,   'Phần',  'FOOD',     FALSE, ARRAY['RESTAURANT'], 45),
('Cơm chiên dương châu',   '🍳', 50000,   'Dĩa',   'FOOD',     FALSE, ARRAY['RESTAURANT'], 46),
('Lẩu thái hải sản',       '🍲', 150000,  'Nồi',   'FOOD',     FALSE, ARRAY['RESTAURANT'], 47),
('Bún riêu cua',           '🍜', 55000,   'Tô',    'FOOD',     FALSE, ARRAY['RESTAURANT'], 48),
('Hủ tiếu Nam Vang',       '🍜', 60000,   'Tô',    'FOOD',     FALSE, ARRAY['RESTAURANT'], 49),

-- ── COFFEE_SHOP ──────────────────────────────────────────────
('Cà phê sữa đá',          '☕', 30000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 50),
('Bạc xỉu',                '☕', 28000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 51),
('Cà phê đen đá',          '☕', 25000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 52),
('Trà sữa trân châu',      '🧋', 45000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 53),
('Americano',              '☕', 55000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 54),
('Latte',                  '☕', 65000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 55),
('Cappuccino',             '☕', 65000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 56),
('Nước ép cam',            '🍊', 40000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP','RESTAURANT'], 57),
('Sinh tố bơ',             '🥑', 55000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP','RESTAURANT'], 58),
('Nước ép dứa',            '🍍', 40000,   'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP','RESTAURANT'], 59),
('Bánh croissant',         '🥐', 35000,   'Cái',   'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 60),
('Sandwich',               '🥪', 45000,   'Cái',   'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 61),
('Bánh tiramisu',          '🍰', 55000,   'Cái',   'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 62),

-- ── FASHION ──────────────────────────────────────────────────
('Áo thun basic',          '👕', 150000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 70),
('Quần jean',              '👖', 350000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 71),
('Áo sơ mi',               '👔', 250000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 72),
('Đầm nữ',                 '👗', 280000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 73),
('Áo khoác',               '🧥', 450000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 74),
('Quần short',             '🩳', 180000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 75),
('Áo dài',                 '👘', 800000,  'Bộ',    'CLOTHING', FALSE, ARRAY['FASHION'], 76),
('Áo len',                 '🧣', 300000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 77),
('Giày sneaker',           '👟', 650000,  'Đôi',   'CLOTHING', FALSE, ARRAY['FASHION'], 78),
('Dép lê',                 '🩴', 150000,  'Đôi',   'CLOTHING', FALSE, ARRAY['FASHION'], 79),
('Túi xách',               '👜', 350000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 80),
('Mũ lưỡi trai',           '🧢', 120000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 81),
('Tất vớ',                 '🧦', 25000,   'Đôi',   'CLOTHING', FALSE, ARRAY['FASHION'], 82),
('Thắt lưng',              '🪡', 150000,  'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 83),
('Kính mắt thời trang',    '🕶️', 200000, 'Cái',   'CLOTHING', FALSE, ARRAY['FASHION'], 84),

-- ── ELECTRONICS ──────────────────────────────────────────────
('Điện thoại smartphone',  '📱', 8000000, 'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS','PAWN_SHOP'], 90),
('Laptop',                 '💻', 15000000,'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS','PAWN_SHOP'], 91),
('Máy tính bảng',          '📱', 6000000, 'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS','PAWN_SHOP'], 92),
('Tai nghe bluetooth',     '🎧', 500000,  'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 93),
('Loa bluetooth',          '🔊', 800000,  'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 94),
('Đồng hồ thông minh',     '⌚', 2500000, 'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS','PAWN_SHOP'], 95),
('Pin sạc dự phòng',       '🔋', 350000,  'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 96),
('Ốp lưng điện thoại',     '📱', 50000,   'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 97),
('Bàn phím không dây',     '⌨️', 400000, 'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 98),
('Chuột máy tính',         '🖱️', 200000, 'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 99),
('Màn hình máy tính',      '🖥️', 3500000,'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 100),
('Camera giám sát',        '📷', 1200000, 'Cái',   'ELECTRONICS', FALSE, ARRAY['ELECTRONICS'], 101),

-- ── BARBER_SHOP (services) ───────────────────────────────────
('Cắt tóc nam',            '💇‍♂️', 50000,   'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 110),
('Cắt tóc nữ',             '💇‍♂️', 100000,  'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 111),
('Cắt tóc trẻ em',         '💇‍♂️', 40000,   'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 112),
('Nhuộm tóc',              '💈', 250000,  'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 113),
('Uốn tóc',                '💈', 350000,  'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 114),
('Duỗi tóc',               '💈', 400000,  'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 115),
('Gội đầu',                '💆', 50000,   'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 116),
('Cạo râu',                '🪒', 30000,   'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 117),
('Massage đầu',            '💆', 80000,   'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 118),
('Phục hồi tóc',           '💇', 200000,  'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 119),
('Tạo kiểu tóc',           '💇', 150000,  'Lần',   'BEAUTY',   FALSE, ARRAY['BARBER_SHOP'], 120),

-- ── PHARMACY ─────────────────────────────────────────────────
('Paracetamol 500mg',      '💊', 15000,   'Hộp',   'DRUG',     FALSE, ARRAY['PHARMACY'], 130),
('Vitamin C 1000mg',       '💊', 55000,   'Hộp',   'DRUG',     FALSE, ARRAY['PHARMACY'], 131),
('Vitamin tổng hợp',       '💊', 85000,   'Hộp',   'DRUG',     FALSE, ARRAY['PHARMACY'], 132),
('Khẩu trang y tế',        '😷', 25000,   'Hộp',   'DRUG',     FALSE, ARRAY['PHARMACY'], 133),
('Nước muối sinh lý 0.9%', '💧', 12000,   'Chai',  'DRUG',     FALSE, ARRAY['PHARMACY'], 134),
('Thuốc ho bổ phế',        '💊', 45000,   'Lọ',    'DRUG',     FALSE, ARRAY['PHARMACY'], 135),
('Băng y tế',              '🩹', 20000,   'Cuộn',  'DRUG',     FALSE, ARRAY['PHARMACY'], 136),
('Băng dán vết thương',    '🩹', 15000,   'Hộp',   'DRUG',     FALSE, ARRAY['PHARMACY'], 137),
('Dầu xoa nóng',           '🧴', 35000,   'Chai',  'DRUG',     FALSE, ARRAY['PHARMACY'], 138),
('Dầu khuynh diệp',        '🧴', 25000,   'Chai',  'DRUG',     FALSE, ARRAY['PHARMACY'], 139),
('Nhiệt kế điện tử',       '🌡️', 250000, 'Cái',   'HEALTH',   FALSE, ARRAY['PHARMACY'], 140),
('Máy đo huyết áp',        '❤️', 800000,  'Cái',   'HEALTH',   FALSE, ARRAY['PHARMACY'], 141),
('Bông y tế',              '🩹', 15000,   'Gói',   'DRUG',     FALSE, ARRAY['PHARMACY'], 142),
('Cồn y tế 90°',           '🧪', 18000,   'Chai',  'DRUG',     FALSE, ARRAY['PHARMACY'], 143),
('Thuốc nhỏ mắt',          '💧', 30000,   'Lọ',    'DRUG',     FALSE, ARRAY['PHARMACY'], 144),

-- ── JEWELRY (dynamic price — priced by gold market rate) ──────
('Nhẫn vàng 18K',          '💍', 0, 'Cái',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 150),
('Nhẫn vàng 24K',          '💍', 0, 'Cái',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 151),
('Nhẫn cưới vàng 18K',     '💍', 0, 'Cái',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 152),
('Dây chuyền vàng 18K',    '⛓️', 0, 'Cái',  'JEWELRY', TRUE,  ARRAY['JEWELRY'], 153),
('Dây chuyền vàng 24K',    '⛓️', 0, 'Cái',  'JEWELRY', TRUE,  ARRAY['JEWELRY'], 154),
('Lắc tay vàng 18K',       '📿', 0, 'Cái',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 155),
('Lắc chân vàng 18K',      '📿', 0, 'Cái',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 156),
('Bông tai vàng 18K',      '💎', 0, 'Đôi',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 157),
('Mặt dây chuyền vàng',    '💎', 0, 'Cái',   'JEWELRY', TRUE,  ARRAY['JEWELRY'], 158),
('Nhẫn bạc',               '💍', 350000, 'Cái',   'JEWELRY', FALSE, ARRAY['JEWELRY'], 159),
('Dây chuyền bạc',         '⛓️', 450000,'Cái',   'JEWELRY', FALSE, ARRAY['JEWELRY'], 160),

-- ── PAWN_SHOP (items accepted as collateral) ──────────────────
('Điện thoại (cầm)',       '📱', 0, 'Cái',   'ELECTRONICS', FALSE, ARRAY['PAWN_SHOP'], 170),
('Laptop (cầm)',           '💻', 0, 'Cái',   'ELECTRONICS', FALSE, ARRAY['PAWN_SHOP'], 171),
('Máy tính bảng (cầm)',    '📱', 0, 'Cái',   'ELECTRONICS', FALSE, ARRAY['PAWN_SHOP'], 172),
('Đồng hồ (cầm)',          '⌚', 0, 'Cái',   'APPLIANCES',  FALSE, ARRAY['PAWN_SHOP'], 173),
('Trang sức vàng (cầm)',   '💍', 0, 'Cái',   'JEWELRY',     FALSE, ARRAY['PAWN_SHOP'], 174),
('Xe máy (cầm)',           '🛵', 0, 'Cái',   'BIKE',        FALSE, ARRAY['PAWN_SHOP'], 175),
('Camera / Máy ảnh (cầm)', '📷', 0, 'Cái',   'ELECTRONICS', FALSE, ARRAY['PAWN_SHOP'], 176),
('Tivi (cầm)',             '📺', 0, 'Cái',   'APPLIANCES',  FALSE, ARRAY['PAWN_SHOP'], 177),
('Đồ gia dụng (cầm)',      '🏠', 0, 'Cái',   'APPLIANCES',  FALSE, ARRAY['PAWN_SHOP'], 178),

-- ── OTHER (generic fallback) ──────────────────────────────────
('Sản phẩm',               '📦', 50000,  'Cái',   'CONVENIENCE', FALSE, ARRAY['OTHER'], 190),
('Dịch vụ',                '🛎️',100000, 'Lần',   'HEALTH',     FALSE, ARRAY['OTHER'], 191)

ON CONFLICT (name) DO NOTHING;

-- ════════════════════════════════════════════════════════════
-- Merged from: V008__expense_suggestions.sql
-- ════════════════════════════════════════════════════════════

-- Expense suggestions for onboarding Step 3.
-- shop_types TEXT[]: 'ALL' = universal (shown for every shop type);
--                     otherwise list the specific ShopType codes.

CREATE TABLE IF NOT EXISTS expense_suggestions (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    emoji         VARCHAR(20)  NOT NULL DEFAULT '💰',
    category_code VARCHAR(50)  NOT NULL DEFAULT 'OTHER',
    shop_types    TEXT[]       NOT NULL DEFAULT '{}',
    display_order INT          NOT NULL DEFAULT 0,
    name_en       VARCHAR(200) DEFAULT NULL,
    CONSTRAINT uq_expense_suggestion_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_expense_suggestions_shop_types
    ON expense_suggestions USING GIN(shop_types);

-- ─── UNIVERSAL (all shop types) ─────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Tiền thuê mặt bằng',          '🏠', 'RENT',         '{ALL}', 1),
  ('Tiền điện',                    '⚡', 'ELECTRICITY',  '{ALL}', 2),
  ('Tiền nước',                    '💧', 'WATER',        '{ALL}', 3),
  ('Internet / WiFi',              '📶', 'INTERNET',     '{ALL}', 4),
  ('Tiền điện thoại',              '📱', 'PHONE',        '{ALL}', 5),
  ('Lương nhân viên',              '👥', 'SALARY_EXTRA', '{ALL}', 6),
  ('Vệ sinh cửa hàng',             '🧹', 'CLEANING',     '{ALL}', 7),
  ('Sửa chữa / bảo trì',           '🔩', 'MAINTENANCE',  '{ALL}', 8),
  ('Phí phần mềm quản lý',         '💻', 'SOFTWARE',     '{ALL}', 9),
  ('Chi phí quảng cáo / fanpage',  '📣', 'MARKETING',    '{ALL}', 10),
  ('Phí ngân hàng / chuyển khoản', '🏦', 'BANK_FEE',     '{ALL}', 11),
  ('Bảo hiểm cửa hàng',            '🛡️', 'INSURANCE',    '{ALL}', 12),
  ('Thuế môn bài / phí kinh doanh','🏛️', 'TAX',          '{ALL}', 13),
  ('Camera / thiết bị an ninh',    '🎥', 'EQUIPMENT',    '{ALL}', 14),
  ('In ấn / văn phòng phẩm',       '🖨️', 'SUPPLIES',     '{ALL}', 15),
  ('Trang trí / nội thất cửa hàng','🪑', 'EQUIPMENT',    '{ALL}', 16),
  ('Đồng phục nhân viên',          '👕', 'SALARY_EXTRA', '{ALL}', 17),
  ('Ăn uống nhân viên',            '🍜', 'FOOD_STAFF',   '{ALL}', 18),
  ('Chi phí giao hàng',            '🛵', 'TRANSPORT',    '{ALL}', 19),
  ('Bao bì / túi đựng',            '🛍️', 'PACKAGING',    '{ALL}', 20)
ON CONFLICT (name) DO NOTHING;

-- ─── FOOD & BEVERAGE / RESTAURANT / COFFEE SHOP ─────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Nguyên liệu / thực phẩm',      '🥩', 'SUPPLIES',   '{FOOD_BEVERAGE,RESTAURANT,COFFEE_SHOP}', 1),
  ('Gas / nhiên liệu nấu ăn',       '🔥', 'EQUIPMENT',  '{FOOD_BEVERAGE,RESTAURANT,COFFEE_SHOP}', 2),
  ('Dụng cụ bếp / nhà hàng',        '🍳', 'EQUIPMENT',  '{FOOD_BEVERAGE,RESTAURANT,COFFEE_SHOP}', 3),
  ('Nguyên liệu cà phê / trà',      '☕', 'SUPPLIES',   '{COFFEE_SHOP,RESTAURANT}',              4),
  ('Ly / cốc / đồ pha chế',         '🧋', 'PACKAGING',  '{COFFEE_SHOP,FOOD_BEVERAGE,RESTAURANT}', 5),
  ('Phí hoa hồng ứng dụng giao đồ ăn','🛵','TRANSPORT', '{FOOD_BEVERAGE,RESTAURANT,COFFEE_SHOP}', 6)
ON CONFLICT (name) DO NOTHING;

-- ─── PHARMACY ────────────────────────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Tủ lạnh bảo quản thuốc',           '❄️', 'EQUIPMENT', '{PHARMACY}', 1),
  ('Phí kiểm định / giấy phép dược phẩm','📋', 'TAX',      '{PHARMACY}', 2),
  ('Bao bì đóng gói thuốc',            '💊', 'PACKAGING', '{PHARMACY}', 3)
ON CONFLICT (name) DO NOTHING;

-- ─── FASHION ─────────────────────────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Phí sàn thương mại điện tử',   '🛒', 'MARKETING', '{FASHION}', 1),
  ('Móc treo / giá trưng bày',      '🪝', 'EQUIPMENT', '{FASHION}', 2),
  ('Bao bì / túi thời trang',       '🛍️', 'PACKAGING', '{FASHION}', 3)
ON CONFLICT (name) DO NOTHING;

-- ─── ELECTRONICS ─────────────────────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Linh kiện / phụ kiện thay thế',        '⚙️', 'SUPPLIES',    '{ELECTRONICS}', 1),
  ('Chi phí bảo hành / dịch vụ sau bán',   '🔧', 'MAINTENANCE', '{ELECTRONICS}', 2)
ON CONFLICT (name) DO NOTHING;

-- ─── JEWELRY / PAWN SHOP ─────────────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Chi phí giám định hàng hóa',        '💎', 'EQUIPMENT', '{JEWELRY,PAWN_SHOP}', 1),
  ('Két sắt / thiết bị bảo mật',        '🔐', 'EQUIPMENT', '{JEWELRY,PAWN_SHOP}', 2),
  ('Phí bảo hiểm hàng quý giá',         '🛡️', 'INSURANCE', '{JEWELRY,PAWN_SHOP}', 3)
ON CONFLICT (name) DO NOTHING;

-- ─── BARBER SHOP / SALON ─────────────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Vật tư dịch vụ (dao, kéo, hóa chất)', '💈', 'SUPPLIES',     '{BARBER_SHOP}', 1),
  ('Khăn / đồ vệ sinh cá nhân',           '🧴', 'CLEANING',     '{BARBER_SHOP}', 2),
  ('Bảo trì / thuê ghế cắt tóc',          '💺', 'MAINTENANCE',  '{BARBER_SHOP}', 3)
ON CONFLICT (name) DO NOTHING;

-- ─── CONVENIENCE STORE ───────────────────────────────────────────────────────

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Phí kiểm kho / kiểm đếm hàng', '📋', 'SUPPLIES',  '{CONVENIENCE_STORE}', 1),
  ('Túi nilon / bao bì siêu thị',   '🛍️', 'PACKAGING', '{CONVENIENCE_STORE}', 2)
ON CONFLICT (name) DO NOTHING;

-- ════════════════════════════════════════════════════════════
-- Merged from: V009__product_duration_minutes.sql
-- ════════════════════════════════════════════════════════════

-- Add duration_minutes to products for timed services (e.g. barber shop)
ALTER TABLE product
    ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 0;


-- ════════════════════════════════════════════════════════════
-- Merged from: V002__add_setup_complete_to_tenant.sql
-- (Column already added to CREATE TABLE tenants above)
-- ════════════════════════════════════════════════════════════
-- setup_complete BOOLEAN NOT NULL DEFAULT TRUE has been added
-- directly to the CREATE TABLE tenants definition above.

-- ════════════════════════════════════════════════════════════
-- Merged from: V003__product_suggestions_category.sql
-- (category_name column added to CREATE TABLE product_suggestions above)
-- ════════════════════════════════════════════════════════════

-- ── Backfill BARBER_SHOP category_name values ─────────────────
UPDATE product_suggestions SET category_name = 'Cắt tóc nam'        WHERE name = 'Cắt tóc nam';
UPDATE product_suggestions SET category_name = 'Cắt tóc nữ'         WHERE name = 'Cắt tóc nữ';
UPDATE product_suggestions SET category_name = 'Cắt tóc nam'        WHERE name = 'Cắt tóc trẻ em';
UPDATE product_suggestions SET category_name = 'Nhuộm & Uốn'        WHERE name = 'Nhuộm tóc';
UPDATE product_suggestions SET category_name = 'Nhuộm & Uốn'        WHERE name = 'Uốn tóc';
UPDATE product_suggestions SET category_name = 'Nhuộm & Uốn'        WHERE name = 'Duỗi tóc';
UPDATE product_suggestions SET category_name = 'Gội đầu & Massage'  WHERE name = 'Gội đầu';
UPDATE product_suggestions SET category_name = 'Chăm sóc râu'       WHERE name = 'Cạo râu';
UPDATE product_suggestions SET category_name = 'Gội đầu & Massage'  WHERE name = 'Massage đầu';
UPDATE product_suggestions SET category_name = 'Gội đầu & Massage'  WHERE name = 'Phục hồi tóc';
UPDATE product_suggestions SET category_name = 'Tạo kiểu & Combo'   WHERE name = 'Tạo kiểu tóc';

-- ── NAIL_SHOP suggestions ─────────────────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Sơn màu thường (tay)',    '💅', 80000,   'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 200, 'Sơn móng thường'),
('Sơn màu thường (chân)',   '💅', 70000,   'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 201, 'Sơn móng thường'),
('Sơn French',             '💅', 100000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 202, 'Sơn móng thường'),
('Sơn gel (tay)',          '💅', 150000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 210, 'Gel & Acrylic'),
('Sơn gel (chân)',         '💅', 120000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 211, 'Gel & Acrylic'),
('Đắp bột acrylic',       '💅', 300000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 212, 'Gel & Acrylic'),
('Đắp gel builder',       '💅', 280000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 213, 'Gel & Acrylic'),
('Tháo gel / Tháo bột',   '💅', 80000,   'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 214, 'Gel & Acrylic'),
('Vẽ nail',               '🎨', 200000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 220, 'Vẽ nail & Nghệ thuật'),
('Nail art',              '🎨', 300000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 221, 'Vẽ nail & Nghệ thuật'),
('Đính đá nail',          '💎', 150000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 222, 'Vẽ nail & Nghệ thuật'),
('Nail ombre',            '🎨', 250000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 223, 'Vẽ nail & Nghệ thuật'),
('Manicure',              '✋', 100000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 230, 'Chăm sóc bàn tay'),
('Dưỡng ẩm tay',         '🧴', 80000,   'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 231, 'Chăm sóc bàn tay'),
('Pedicure',              '🦶', 130000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 240, 'Chăm sóc bàn chân'),
('Tẩy da chết chân',     '🦶', 100000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 241, 'Chăm sóc bàn chân'),
('Combo tay + chân',     '💅', 130000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 250, 'Combo & Gói dịch vụ'),
('Combo gel tay + chân', '💅', 250000,  'Lần',   'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 251, 'Combo & Gói dịch vụ')
ON CONFLICT (name) DO NOTHING;

-- ── SPA_SHOP suggestions ──────────────────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Massage thư giãn 60p',   '💆', 350000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 300, 'Massage'),
('Massage thư giãn 90p',   '💆', 500000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 301, 'Massage'),
('Massage đầu & cổ',      '💆', 200000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 302, 'Massage'),
('Massage bàn chân',      '🦶', 200000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 303, 'Massage'),
('Massage đá nóng',       '🪨', 600000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 304, 'Massage'),
('Massage tinh dầu',      '🌿', 450000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 305, 'Massage'),
('Chăm sóc da mặt cơ bản','✨', 250000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 310, 'Chăm sóc da mặt'),
('Chăm sóc da mặt chuyên sâu','✨', 400000, 'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 311, 'Chăm sóc da mặt'),
('Nặn mụn',              '🫧', 300000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 312, 'Chăm sóc da mặt'),
('Đắp mặt nạ dưỡng ẩm',  '🎭', 150000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 313, 'Chăm sóc da mặt'),
('Tẩy tế bào chết toàn thân','🧖', 300000,'Lần',  'SERVICE', FALSE, ARRAY['SPA_SHOP'], 320, 'Chăm sóc cơ thể'),
('Ủ trắng toàn thân',    '🧖', 400000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 321, 'Chăm sóc cơ thể'),
('Wax lông nách',        '✂️', 80000,   'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 330, 'Waxing & Triệt lông'),
('Wax lông chân',        '✂️', 200000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 331, 'Waxing & Triệt lông'),
('Wax bikini',           '✂️', 200000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 332, 'Waxing & Triệt lông'),
('Trị nám, tàn nhang',   '✨', 500000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 340, 'Điều trị đặc biệt'),
('Trị mụn lưng',        '🫧', 350000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 341, 'Điều trị đặc biệt'),
('Combo mặt + massage',  '💆', 600000,  'Lần',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 350, 'Combo & Liệu trình'),
('Liệu trình 5 buổi',   '📋', 1800000, 'Gói',   'SERVICE', FALSE, ARRAY['SPA_SHOP'], 351, 'Combo & Liệu trình')
ON CONFLICT (name) DO NOTHING;

-- ── NAIL_SHOP expense suggestions ─────────────────────────────
INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Sơn / gel / bột nail',            '💅', 'SUPPLIES',    '{NAIL_SHOP}', 1),
  ('Đèn UV / máy khoan nail',         '🔧', 'EQUIPMENT',   '{NAIL_SHOP}', 2),
  ('Khăn / bông tẩy trang / phụ kiện','🧴', 'CLEANING',    '{NAIL_SHOP}', 3)
ON CONFLICT (name) DO NOTHING;

-- ── SPA_SHOP expense suggestions ──────────────────────────────
INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Dầu massage / tinh dầu aromatherapy','🌿', 'SUPPLIES',   '{SPA_SHOP}', 1),
  ('Kem dưỡng / mặt nạ / vật tư spa',   '🧴', 'SUPPLIES',   '{SPA_SHOP}', 2),
  ('Khăn / đồ vải spa',                 '🛁', 'CLEANING',   '{SPA_SHOP}', 3),
  ('Máy massage / thiết bị spa',        '🔧', 'EQUIPMENT',  '{SPA_SHOP}', 4)
ON CONFLICT (name) DO NOTHING;

-- ════════════════════════════════════════════════════════════
-- Merged from: V004__exchange_rate_history.sql
-- ════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS exchange_rate_history (
    id          BIGSERIAL PRIMARY KEY,
    currency_code VARCHAR(3)    NOT NULL,
    source        VARCHAR(50)   NOT NULL,
    buy_rate      NUMERIC(18,4),
    transfer_rate NUMERIC(18,4),
    sell_rate     NUMERIC(18,4),
    fetched_at    TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_erh_currency_fetched
    ON exchange_rate_history (currency_code, fetched_at DESC);

CREATE INDEX IF NOT EXISTS idx_erh_fetched_at
    ON exchange_rate_history (fetched_at DESC);

-- ════════════════════════════════════════════════════════════
-- Merged from: V005__market_gold_prices.sql
-- ════════════════════════════════════════════════════════════

-- Market gold price cache — one row per (ktype, source), upserted every poll cycle
CREATE TABLE IF NOT EXISTS market_gold_prices (
    ktype       VARCHAR(30)   NOT NULL,
    source      VARCHAR(20)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    buy_price   NUMERIC(20,0),
    sell_price  NUMERIC(20,0),
    fetched_at  TIMESTAMP     NOT NULL,
    PRIMARY KEY (ktype, source)
);

-- Full history — appended on every poll, pruned weekly (90-day retention)
CREATE TABLE IF NOT EXISTS market_gold_price_history (
    id          BIGSERIAL     PRIMARY KEY,
    ktype       VARCHAR(30)   NOT NULL,
    source      VARCHAR(20)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    buy_price   NUMERIC(20,0),
    sell_price  NUMERIC(20,0),
    fetched_at  TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mgph_source_ktype_fetched
    ON market_gold_price_history (source, ktype, fetched_at DESC);

CREATE INDEX IF NOT EXISTS idx_mgph_fetched_at
    ON market_gold_price_history (fetched_at DESC);

-- ════════════════════════════════════════════════════════════
-- Merged from: V006__appointments.sql
-- Bug fix: original used INSERT INTO features (code, name, description)
-- but features table has no 'code' column. Fixed to use explicit id.
-- ════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS appointments (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           VARCHAR(50)  NOT NULL,
    appointment_number  VARCHAR(20)  NOT NULL,
    customer_id         BIGINT,
    customer_name       VARCHAR(255) NOT NULL,
    customer_phone      VARCHAR(20),
    scheduled_date      DATE         NOT NULL,
    scheduled_start_time TIME        NOT NULL,
    duration_minutes    INT          NOT NULL DEFAULT 60,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    note                TEXT,
    linked_order_id     BIGINT,
    created_by          VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS appointment_services (
    id                      BIGSERIAL PRIMARY KEY,
    appointment_id          BIGINT       NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    product_id              BIGINT       NOT NULL,
    product_name            VARCHAR(255) NOT NULL,
    unit_price              DECIMAL(15,2) NOT NULL DEFAULT 0,
    duration_minutes        INT          NOT NULL DEFAULT 0,
    assigned_employee_id    BIGINT,
    assigned_employee_name  VARCHAR(255)
);

-- RLS policies
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments FORCE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'appointments' AND policyname = 'appointments_tenant_isolation'
    ) THEN
        CREATE POLICY appointments_tenant_isolation ON appointments
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

-- Unique appointment number per tenant
CREATE UNIQUE INDEX IF NOT EXISTS idx_appointments_number
    ON appointments (tenant_id, appointment_number)
    WHERE deleted = FALSE;

-- Fast lookup by date
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_date
    ON appointments (tenant_id, scheduled_date)
    WHERE deleted = FALSE;

-- APPOINTMENT feature (id=202601038)
INSERT INTO features (id, name, display_name, description, active, deleted)
VALUES (202601038, 'APPOINTMENT', 'Lịch Hẹn', 'Quản lý lịch hẹn với khách hàng, đặt lịch và xác nhận', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('features', 'id'), 202601038, true);

-- ════════════════════════════════════════════════════════════
-- Merged from: V007__beauty_shop_suggestions.sql
-- ════════════════════════════════════════════════════════════

-- ── BARBER_SHOP_MEN product suggestions ──────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Cắt tóc thường (nam)',        '💇', 80000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 400, 'Cắt tóc'),
('Cắt Fade',                    '💇', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 401, 'Cắt tóc'),
('Cắt Undercut',                '💇', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 402, 'Cắt tóc'),
('Cắt tóc trẻ em (nam)',        '👦', 60000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 403, 'Cắt tóc'),
('Cạo râu thường',              '🪒', 50000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 410, 'Cạo & Chăm sóc râu'),
('Cạo râu + định hình râu',     '🪒', 80000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 411, 'Cạo & Chăm sóc râu'),
('Trim & tỉa râu',              '🪒', 40000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 412, 'Cạo & Chăm sóc râu'),
('Gội đầu + massage đầu (nam)', '💆', 80000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 420, 'Gội đầu & Massage'),
('Massage đầu cổ vai 20p',      '💆', 100000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 421, 'Gội đầu & Massage'),
('Tạo kiểu sáp / wax tóc',     '✨', 50000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 430, 'Tạo kiểu'),
('Nhuộm tóc nam',               '💈', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 431, 'Tạo kiểu'),
('Combo cắt + cạo râu',         '💈', 180000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 440, 'Combo'),
('Combo cắt + gội + massage đầu','💈', 200000, 'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 441, 'Combo')
ON CONFLICT (name) DO NOTHING;

-- ── HAIR_SALON product suggestions ───────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Cắt tóc nữ ngắn',              '✂️', 120000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 450, 'Cắt tóc'),
('Cắt tóc nữ dài',               '✂️', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 451, 'Cắt tóc'),
('Cắt tỉa layer',                '✂️', 130000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 452, 'Cắt tóc'),
('Nhuộm màu thời trang',         '🎨', 400000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 460, 'Nhuộm tóc'),
('Nhuộm highlight / ombre',      '🎨', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 461, 'Nhuộm tóc'),
('Nhuộm phủ bạc',                '🎨', 300000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 462, 'Nhuộm tóc'),
('Uốn xoăn Hàn Quốc',           '💫', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 470, 'Uốn & Duỗi'),
('Duỗi phồng / duỗi thẳng',     '💫', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 471, 'Uốn & Duỗi'),
('Ép tóc Keratin',               '💫', 700000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 472, 'Uốn & Duỗi'),
('Ủ phục hồi tóc hư tổn',       '🌿', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 480, 'Chăm sóc tóc'),
('Gội đầu dưỡng + massage đầu', '💆', 100000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 490, 'Gội đầu & Massage'),
('Tạo kiểu đi tiệc / sự kiện',  '✨', 300000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 500, 'Tạo kiểu & Combo'),
('Combo cắt + nhuộm tóc',       '💈', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 501, 'Tạo kiểu & Combo')
ON CONFLICT (name) DO NOTHING;

-- ── LASH_PMU_STUDIO product suggestions ──────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Nối mi cơ bản',               '👁', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 510, 'Nối mi'),
('Nối mi volume',               '👁', 350000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 511, 'Nối mi'),
('Nối mi mega volume',          '👁', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 512, 'Nối mi'),
('Xăm mày tán bột / ombre',    '✏', 2000000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 520, 'Xăm mày'),
('Xăm mày giả lông',           '✏', 2500000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 521, 'Xăm mày'),
('Xăm môi bóng / ombre',       '💋', 3000000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 530, 'Xăm môi'),
('Xăm mí mắt trên',            '👁', 1500000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 540, 'Xăm mí mắt'),
('Tháo mi',                    '✂', 100000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 550, 'Chăm sóc & Tháo'),
('Điều chỉnh / fill mi',       '👁', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 551, 'Chăm sóc & Tháo'),
('Dưỡng phục hồi sau xăm',     '🌿', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 552, 'Chăm sóc & Tháo'),
('Combo nối mi + fill mi',     '✨', 450000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 560, 'Combo'),
('Combo mày + môi trọn gói',   '✨', 5000000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 561, 'Combo')
ON CONFLICT (name) DO NOTHING;

-- ── MASSAGE_SHOP product suggestions ─────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Massage thư giãn toàn thân 60p', '💆', 200000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 570, 'Massage toàn thân'),
('Massage toàn thân 90p',          '💆', 280000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 571, 'Massage toàn thân'),
('Massage toàn thân 120p',         '💆', 350000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 572, 'Massage toàn thân'),
('Massage chân phản xạ 30p',       '🦶', 100000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 580, 'Massage chân phản xạ'),
('Massage chân phản xạ 60p',       '🦶', 180000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 581, 'Massage chân phản xạ'),
('Massage đầu vai gáy 30p',        '💆', 100000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 590, 'Massage đầu & vai gáy'),
('Massage lưng & cổ 30p',          '💆', 150000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 600, 'Massage lưng & cổ'),
('Xông hơi ướt',                   '🌊', 100000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 610, 'Xông hơi & Ngâm'),
('Ngâm chân thảo dược',            '🌿', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 611, 'Xông hơi & Ngâm'),
('Combo massage + ngâm chân',      '✨', 250000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 620, 'Combo'),
('Combo toàn thân + xông hơi',     '✨', 400000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 621, 'Combo')
ON CONFLICT (name) DO NOTHING;

-- ── BEAUTY_CLINIC product suggestions ────────────────────────
UPDATE product_suggestions
SET shop_types = array_append(shop_types, 'BEAUTY_CLINIC')
WHERE name IN (
    'Chăm sóc da mặt cơ bản',
    'Chăm sóc da mặt chuyên sâu',
    'Đắp mặt nạ dưỡng ẩm',
    'Tẩy tế bào chết toàn thân',
    'Ủ trắng toàn thân',
    'Wax lông nách',
    'Wax lông chân',
    'Trị nám, tàn nhang'
)
  AND NOT ('BEAUTY_CLINIC' = ANY(shop_types));

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Nặn mụn an toàn tại thẩm mỹ viện', '🫧', 300000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 630, 'Trị mụn & Nám'),
('Trị mụn bằng laser',               '💡', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 631, 'Trị mụn & Nám'),
('Laser trẻ hóa da',                 '💡', 1200000, 'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 640, 'Công nghệ thẩm mỹ'),
('RF nâng cơ / căng da',             '⚡', 1500000, 'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 641, 'Công nghệ thẩm mỹ'),
('HIFU nâng cơ không phẫu thuật',    '💡', 2000000, 'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 642, 'Công nghệ thẩm mỹ'),
('Triệt lông laser (1 vùng)',        '💡', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 650, 'Waxing & Triệt lông'),
('Combo liệu trình da 5 buổi',       '📋', 1500000, 'Gói', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 660, 'Combo & Liệu trình')
ON CONFLICT (name) DO NOTHING;

-- ── MAKEUP_STUDIO product suggestions ────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name)
VALUES
('Trang điểm nhẹ nhàng hàng ngày', '💄', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 670, 'Trang điểm ngày thường'),
('Trang điểm Hàn Quốc (K-makeup)', '💄', 300000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 671, 'Trang điểm ngày thường'),
('Trang điểm đi tiệc ban ngày',    '💄', 400000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 680, 'Trang điểm đi tiệc'),
('Trang điểm dự tiệc tối / event', '💄', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 681, 'Trang điểm đi tiệc'),
('Trang điểm tốt nghiệp',          '🎓', 350000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 682, 'Trang điểm đi tiệc'),
('Trang điểm chụp ảnh',            '📸', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 683, 'Trang điểm đi tiệc'),
('Trang điểm cô dâu thử (trial)',  '👰', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 690, 'Trang điểm cô dâu'),
('Trang điểm cô dâu ngày cưới',   '👰', 1500000, 'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 691, 'Trang điểm cô dâu'),
('Trang điểm phụ dâu / phù rể',   '💍', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 692, 'Trang điểm cô dâu'),
('Búi tóc đơn giản',               '💇', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 700, 'Làm tóc & Phụ kiện'),
('Tạo kiểu tóc đi tiệc / sự kiện','✨', 300000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 701, 'Làm tóc & Phụ kiện'),
('Combo trang điểm + tóc tiệc',   '✨', 700000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 710, 'Combo & Gói cưới'),
('Gói cưới cô dâu cơ bản',        '👰', 2500000, 'Gói', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 711, 'Combo & Gói cưới')
ON CONFLICT (name) DO NOTHING;

-- ── Beauty shop expense suggestions ──────────────────────────
INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Vật tư cắt tóc (tông đơ, dao, kéo)',  '💈', 'SUPPLIES',    '{BARBER_SHOP_MEN}', 1),
  ('Dầu cạo râu / kem cạo râu',           '🪒', 'SUPPLIES',    '{BARBER_SHOP_MEN}', 2),
  ('Khăn bông / khăn lạnh phục vụ',       '🧴', 'CLEANING',    '{BARBER_SHOP_MEN}', 3),
  ('Bảo trì ghế cắt / thiết bị salon',   '💺', 'MAINTENANCE', '{BARBER_SHOP_MEN}', 4)
ON CONFLICT (name) DO NOTHING;

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Thuốc nhuộm / thuốc uốn / hóa chất tóc', '🎨', 'SUPPLIES',    '{HAIR_SALON}', 1),
  ('Dầu gội / dầu xả chuyên nghiệp',          '🧴', 'SUPPLIES',    '{HAIR_SALON}', 2),
  ('Khăn bông / áo choàng khách',             '🧺', 'CLEANING',    '{HAIR_SALON}', 3),
  ('Bảo trì máy sấy / máy uốn / máy duỗi',   '🔧', 'MAINTENANCE', '{HAIR_SALON}', 4)
ON CONFLICT (name) DO NOTHING;

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Chỉ mi / keo mi / dung môi tháo keo',  '👁', 'SUPPLIES',    '{LASH_PMU_STUDIO}', 1),
  ('Mực xăm / kim xăm tiêu hao',           '✏', 'SUPPLIES',    '{LASH_PMU_STUDIO}', 2),
  ('Khăn vô trùng / vật tư tiệt khuẩn',   '🧤', 'CLEANING',    '{LASH_PMU_STUDIO}', 3),
  ('Bảo trì giường / ghế kỹ thuật viên',  '🛋', 'MAINTENANCE', '{LASH_PMU_STUDIO}', 4)
ON CONFLICT (name) DO NOTHING;

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Tinh dầu / dầu massage chuyên dụng',  '🌿', 'SUPPLIES',    '{MASSAGE_SHOP}', 1),
  ('Khăn bông / đồ vải massage',          '🛁', 'CLEANING',    '{MASSAGE_SHOP}', 2),
  ('Bảo trì giường massage',              '🛏', 'MAINTENANCE', '{MASSAGE_SHOP}', 3),
  ('Đá bazan / thiết bị nhiệt massage',   '🪨', 'EQUIPMENT',   '{MASSAGE_SHOP}', 4)
ON CONFLICT (name) DO NOTHING;

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Hóa chất / serum / ampoule điều trị', '🧪', 'SUPPLIES',    '{BEAUTY_CLINIC}', 1),
  ('Kim vi kim / đầu mũi khoan tiêu hao', '💉', 'SUPPLIES',    '{BEAUTY_CLINIC}', 2),
  ('Khăn vô trùng / vật tư y tế 1 lần',  '🧤', 'CLEANING',    '{BEAUTY_CLINIC}', 3),
  ('Bảo trì thiết bị laser / RF / HIFU',  '🔧', 'MAINTENANCE', '{BEAUTY_CLINIC}', 4),
  ('Phí kiểm định thiết bị thẩm mỹ',     '📋', 'TAX',         '{BEAUTY_CLINIC}', 5)
ON CONFLICT (name) DO NOTHING;

INSERT INTO expense_suggestions (name, emoji, category_code, shop_types, display_order) VALUES
  ('Mỹ phẩm / son / phấn / kem nền',    '💄', 'SUPPLIES',  '{MAKEUP_STUDIO}', 1),
  ('Cọ makeup / dụng cụ trang điểm',   '🖌', 'SUPPLIES',  '{MAKEUP_STUDIO}', 2),
  ('Đèn ring light / ghế trang điểm',  '💡', 'EQUIPMENT', '{MAKEUP_STUDIO}', 3),
  ('Áo choàng / khăn phục vụ khách',   '🧺', 'CLEANING',  '{MAKEUP_STUDIO}', 4)
ON CONFLICT (name) DO NOTHING;

-- ════════════════════════════════════════════════════════════
-- Merged from: V008__suggestions_name_en.sql
-- (name_en columns already added to CREATE TABLE definitions above)
-- ════════════════════════════════════════════════════════════

-- ── product_suggestions: English translations ─────────────────
-- Shared beverages
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Nước suối'               THEN 'Mineral Water'
    WHEN 'Coca Cola'               THEN 'Coca Cola'
    WHEN 'Pepsi'                   THEN 'Pepsi'
    WHEN 'Bia Saigon'              THEN 'Saigon Beer'
    WHEN 'Bia Tiger'               THEN 'Tiger Beer'
    WHEN 'Trứng gà'                THEN 'Eggs'
    WHEN 'Bánh mì'                 THEN 'Bread'
    ELSE name_en END
WHERE name IN ('Nước suối','Coca Cola','Pepsi','Bia Saigon','Bia Tiger','Trứng gà','Bánh mì');

-- CONVENIENCE_STORE
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Mì tôm Hảo Hảo'         THEN 'Instant Noodles'
    WHEN 'Sữa tươi Vinamilk'       THEN 'Fresh Milk'
    WHEN 'Dầu ăn'                  THEN 'Cooking Oil'
    WHEN 'Gạo'                     THEN 'Rice'
    WHEN 'Nước mắm'                THEN 'Fish Sauce'
    WHEN 'Đường cát'               THEN 'Sugar'
    WHEN 'Bột giặt'                THEN 'Laundry Detergent'
    WHEN 'Dầu gội đầu'             THEN 'Shampoo'
    WHEN 'Xà phòng'                THEN 'Soap'
    WHEN 'Khăn giấy'               THEN 'Tissue Paper'
    WHEN 'Thuốc lá'                THEN 'Cigarettes'
    WHEN 'Pin AA'                  THEN 'AA Batteries'
    WHEN 'Sạc điện thoại'          THEN 'Phone Charger'
    WHEN 'Cáp USB'                 THEN 'USB Cable'
    WHEN 'Nước tăng lực Redbull'   THEN 'Red Bull Energy Drink'
    WHEN 'Snack khoai tây'         THEN 'Potato Chips'
    WHEN 'Kẹo cao su'              THEN 'Chewing Gum'
    ELSE name_en END
WHERE name IN ('Mì tôm Hảo Hảo','Sữa tươi Vinamilk','Dầu ăn','Gạo','Nước mắm','Đường cát',
               'Bột giặt','Dầu gội đầu','Xà phòng','Khăn giấy','Thuốc lá','Pin AA',
               'Sạc điện thoại','Cáp USB','Nước tăng lực Redbull','Snack khoai tây','Kẹo cao su');

-- FOOD_BEVERAGE
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Rau cải xanh'            THEN 'Green Vegetables'
    WHEN 'Thịt heo'                THEN 'Pork'
    WHEN 'Thịt bò'                 THEN 'Beef'
    WHEN 'Cá tươi'                 THEN 'Fresh Fish'
    WHEN 'Đậu hũ'                  THEN 'Tofu'
    WHEN 'Cà chua'                 THEN 'Tomatoes'
    WHEN 'Hành tây'                THEN 'Onions'
    WHEN 'Tỏi'                     THEN 'Garlic'
    WHEN 'Cà rốt'                  THEN 'Carrots'
    WHEN 'Dầu hào'                 THEN 'Oyster Sauce'
    ELSE name_en END
WHERE name IN ('Rau cải xanh','Thịt heo','Thịt bò','Cá tươi','Đậu hũ',
               'Cà chua','Hành tây','Tỏi','Cà rốt','Dầu hào');

-- RESTAURANT
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Phở bò'                  THEN 'Beef Pho'
    WHEN 'Cơm tấm sườn'            THEN 'Broken Rice with Grilled Pork'
    WHEN 'Bún bò Huế'              THEN 'Hue Spicy Beef Noodle'
    WHEN 'Bánh mì thịt'            THEN 'Banh Mi Sandwich'
    WHEN 'Gỏi cuốn'                THEN 'Fresh Spring Rolls'
    WHEN 'Chả giò'                 THEN 'Fried Spring Rolls'
    WHEN 'Cơm chiên dương châu'    THEN 'Yang Chow Fried Rice'
    WHEN 'Lẩu thái hải sản'        THEN 'Thai Seafood Hot Pot'
    WHEN 'Bún riêu cua'            THEN 'Crab Noodle Soup'
    WHEN 'Hủ tiếu Nam Vang'        THEN 'Nam Vang Noodle Soup'
    ELSE name_en END
WHERE name IN ('Phở bò','Cơm tấm sườn','Bún bò Huế','Bánh mì thịt','Gỏi cuốn',
               'Chả giò','Cơm chiên dương châu','Lẩu thái hải sản','Bún riêu cua','Hủ tiếu Nam Vang');

-- COFFEE_SHOP
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Cà phê sữa đá'           THEN 'Iced Milk Coffee'
    WHEN 'Bạc xỉu'                 THEN 'Vietnamese White Coffee'
    WHEN 'Cà phê đen đá'           THEN 'Iced Black Coffee'
    WHEN 'Trà sữa trân châu'       THEN 'Bubble Tea'
    WHEN 'Americano'               THEN 'Americano'
    WHEN 'Latte'                   THEN 'Latte'
    WHEN 'Cappuccino'              THEN 'Cappuccino'
    WHEN 'Nước ép cam'             THEN 'Orange Juice'
    WHEN 'Sinh tố bơ'              THEN 'Avocado Smoothie'
    WHEN 'Nước ép dứa'             THEN 'Pineapple Juice'
    WHEN 'Bánh croissant'          THEN 'Croissant'
    WHEN 'Sandwich'                THEN 'Sandwich'
    WHEN 'Bánh tiramisu'           THEN 'Tiramisu Cake'
    ELSE name_en END
WHERE name IN ('Cà phê sữa đá','Bạc xỉu','Cà phê đen đá','Trà sữa trân châu','Americano',
               'Latte','Cappuccino','Nước ép cam','Sinh tố bơ','Nước ép dứa',
               'Bánh croissant','Sandwich','Bánh tiramisu');

-- FASHION
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Áo thun basic'           THEN 'Basic T-Shirt'
    WHEN 'Quần jean'               THEN 'Jeans'
    WHEN 'Áo sơ mi'                THEN 'Dress Shirt'
    WHEN 'Đầm nữ'                  THEN 'Women''s Dress'
    WHEN 'Áo khoác'                THEN 'Jacket'
    WHEN 'Quần short'              THEN 'Shorts'
    WHEN 'Áo dài'                  THEN 'Ao Dai (Traditional Dress)'
    WHEN 'Áo len'                  THEN 'Sweater'
    WHEN 'Giày sneaker'            THEN 'Sneakers'
    WHEN 'Dép lê'                  THEN 'Flip-flops'
    WHEN 'Túi xách'                THEN 'Handbag'
    WHEN 'Mũ lưỡi trai'           THEN 'Baseball Cap'
    WHEN 'Tất vớ'                  THEN 'Socks'
    WHEN 'Thắt lưng'               THEN 'Belt'
    WHEN 'Kính mắt thời trang'     THEN 'Fashion Sunglasses'
    ELSE name_en END
WHERE name IN ('Áo thun basic','Quần jean','Áo sơ mi','Đầm nữ','Áo khoác','Quần short',
               'Áo dài','Áo len','Giày sneaker','Dép lê','Túi xách','Mũ lưỡi trai',
               'Tất vớ','Thắt lưng','Kính mắt thời trang');

-- ELECTRONICS
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Điện thoại smartphone'   THEN 'Smartphone'
    WHEN 'Laptop'                  THEN 'Laptop'
    WHEN 'Máy tính bảng'           THEN 'Tablet'
    WHEN 'Tai nghe bluetooth'      THEN 'Bluetooth Headphones'
    WHEN 'Loa bluetooth'           THEN 'Bluetooth Speaker'
    WHEN 'Đồng hồ thông minh'      THEN 'Smartwatch'
    WHEN 'Pin sạc dự phòng'        THEN 'Power Bank'
    WHEN 'Ốp lưng điện thoại'      THEN 'Phone Case'
    WHEN 'Bàn phím không dây'      THEN 'Wireless Keyboard'
    WHEN 'Chuột máy tính'          THEN 'Computer Mouse'
    WHEN 'Màn hình máy tính'       THEN 'Computer Monitor'
    WHEN 'Camera giám sát'         THEN 'Security Camera'
    ELSE name_en END
WHERE name IN ('Điện thoại smartphone','Laptop','Máy tính bảng','Tai nghe bluetooth',
               'Loa bluetooth','Đồng hồ thông minh','Pin sạc dự phòng','Ốp lưng điện thoại',
               'Bàn phím không dây','Chuột máy tính','Màn hình máy tính','Camera giám sát');

-- BARBER_SHOP
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Cắt tóc nam'             THEN 'Men''s Haircut'
    WHEN 'Cắt tóc nữ'              THEN 'Women''s Haircut'
    WHEN 'Cắt tóc trẻ em'          THEN 'Children''s Haircut'
    WHEN 'Nhuộm tóc'               THEN 'Hair Coloring'
    WHEN 'Uốn tóc'                 THEN 'Hair Perm'
    WHEN 'Duỗi tóc'                THEN 'Hair Straightening'
    WHEN 'Gội đầu'                 THEN 'Hair Wash'
    WHEN 'Cạo râu'                 THEN 'Shave'
    WHEN 'Massage đầu'             THEN 'Head Massage'
    WHEN 'Phục hồi tóc'            THEN 'Hair Treatment'
    WHEN 'Tạo kiểu tóc'            THEN 'Hair Styling'
    ELSE name_en END
WHERE name IN ('Cắt tóc nam','Cắt tóc nữ','Cắt tóc trẻ em','Nhuộm tóc','Uốn tóc',
               'Duỗi tóc','Gội đầu','Cạo râu','Massage đầu','Phục hồi tóc','Tạo kiểu tóc');

-- PHARMACY
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Paracetamol 500mg'       THEN 'Paracetamol 500mg'
    WHEN 'Vitamin C 1000mg'        THEN 'Vitamin C 1000mg'
    WHEN 'Vitamin tổng hợp'        THEN 'Multivitamin'
    WHEN 'Khẩu trang y tế'         THEN 'Medical Face Mask'
    WHEN 'Nước muối sinh lý 0.9%'  THEN 'Saline Solution 0.9%'
    WHEN 'Thuốc ho bổ phế'         THEN 'Cough Medicine'
    WHEN 'Băng y tế'               THEN 'Medical Bandage'
    WHEN 'Băng dán vết thương'     THEN 'Adhesive Bandage'
    WHEN 'Dầu xoa nóng'            THEN 'Heating Rub'
    WHEN 'Dầu khuynh diệp'         THEN 'Eucalyptus Oil'
    WHEN 'Nhiệt kế điện tử'        THEN 'Digital Thermometer'
    WHEN 'Máy đo huyết áp'         THEN 'Blood Pressure Monitor'
    WHEN 'Bông y tế'               THEN 'Medical Cotton'
    WHEN 'Cồn y tế 90°'            THEN 'Rubbing Alcohol 90°'
    WHEN 'Thuốc nhỏ mắt'           THEN 'Eye Drops'
    ELSE name_en END
WHERE name IN ('Paracetamol 500mg','Vitamin C 1000mg','Vitamin tổng hợp','Khẩu trang y tế',
               'Nước muối sinh lý 0.9%','Thuốc ho bổ phế','Băng y tế','Băng dán vết thương',
               'Dầu xoa nóng','Dầu khuynh diệp','Nhiệt kế điện tử','Máy đo huyết áp',
               'Bông y tế','Cồn y tế 90°','Thuốc nhỏ mắt');

-- JEWELRY
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Nhẫn vàng 18K'           THEN '18K Gold Ring'
    WHEN 'Nhẫn vàng 24K'           THEN '24K Gold Ring'
    WHEN 'Nhẫn cưới vàng 18K'      THEN '18K Gold Wedding Ring'
    WHEN 'Dây chuyền vàng 18K'     THEN '18K Gold Necklace'
    WHEN 'Dây chuyền vàng 24K'     THEN '24K Gold Necklace'
    WHEN 'Lắc tay vàng 18K'        THEN '18K Gold Bracelet'
    WHEN 'Lắc chân vàng 18K'       THEN '18K Gold Anklet'
    WHEN 'Bông tai vàng 18K'       THEN '18K Gold Earrings'
    WHEN 'Mặt dây chuyền vàng'     THEN 'Gold Necklace Pendant'
    WHEN 'Nhẫn bạc'                THEN 'Silver Ring'
    WHEN 'Dây chuyền bạc'          THEN 'Silver Necklace'
    ELSE name_en END
WHERE name IN ('Nhẫn vàng 18K','Nhẫn vàng 24K','Nhẫn cưới vàng 18K','Dây chuyền vàng 18K',
               'Dây chuyền vàng 24K','Lắc tay vàng 18K','Lắc chân vàng 18K','Bông tai vàng 18K',
               'Mặt dây chuyền vàng','Nhẫn bạc','Dây chuyền bạc');

-- PAWN_SHOP
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Điện thoại (cầm)'        THEN 'Smartphone (Pawn)'
    WHEN 'Laptop (cầm)'            THEN 'Laptop (Pawn)'
    WHEN 'Máy tính bảng (cầm)'     THEN 'Tablet (Pawn)'
    WHEN 'Đồng hồ (cầm)'           THEN 'Watch (Pawn)'
    WHEN 'Trang sức vàng (cầm)'    THEN 'Gold Jewelry (Pawn)'
    WHEN 'Xe máy (cầm)'            THEN 'Motorbike (Pawn)'
    WHEN 'Camera / Máy ảnh (cầm)'  THEN 'Camera (Pawn)'
    WHEN 'Tivi (cầm)'              THEN 'Television (Pawn)'
    WHEN 'Đồ gia dụng (cầm)'       THEN 'Home Appliance (Pawn)'
    ELSE name_en END
WHERE name IN ('Điện thoại (cầm)','Laptop (cầm)','Máy tính bảng (cầm)','Đồng hồ (cầm)',
               'Trang sức vàng (cầm)','Xe máy (cầm)','Camera / Máy ảnh (cầm)',
               'Tivi (cầm)','Đồ gia dụng (cầm)');

-- OTHER
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Sản phẩm'                THEN 'Product'
    WHEN 'Dịch vụ'                 THEN 'Service'
    ELSE name_en END
WHERE name IN ('Sản phẩm','Dịch vụ');

-- NAIL_SHOP (V003 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Sơn màu thường (tay)'    THEN 'Regular Polish (Hands)'
    WHEN 'Sơn màu thường (chân)'   THEN 'Regular Polish (Feet)'
    WHEN 'Sơn French'              THEN 'French Polish'
    WHEN 'Sơn gel (tay)'           THEN 'Gel Polish (Hands)'
    WHEN 'Sơn gel (chân)'          THEN 'Gel Polish (Feet)'
    WHEN 'Đắp bột acrylic'         THEN 'Acrylic Extensions'
    WHEN 'Đắp gel builder'         THEN 'Gel Builder Extensions'
    WHEN 'Tháo gel / Tháo bột'     THEN 'Gel / Acrylic Removal'
    WHEN 'Vẽ nail'                 THEN 'Nail Art Design'
    WHEN 'Nail art'                THEN 'Nail Art'
    WHEN 'Đính đá nail'            THEN 'Nail Rhinestones'
    WHEN 'Nail ombre'              THEN 'Ombre Nails'
    WHEN 'Manicure'                THEN 'Manicure'
    WHEN 'Dưỡng ẩm tay'           THEN 'Hand Moisturiser Treatment'
    WHEN 'Pedicure'                THEN 'Pedicure'
    WHEN 'Tẩy da chết chân'        THEN 'Foot Exfoliation'
    WHEN 'Combo tay + chân'        THEN 'Hands + Feet Combo'
    WHEN 'Combo gel tay + chân'    THEN 'Gel Hands + Feet Combo'
    ELSE name_en END
WHERE name IN ('Sơn màu thường (tay)','Sơn màu thường (chân)','Sơn French','Sơn gel (tay)',
               'Sơn gel (chân)','Đắp bột acrylic','Đắp gel builder','Tháo gel / Tháo bột',
               'Vẽ nail','Nail art','Đính đá nail','Nail ombre','Manicure','Dưỡng ẩm tay',
               'Pedicure','Tẩy da chết chân','Combo tay + chân','Combo gel tay + chân');

-- SPA_SHOP (V003 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Massage thư giãn 60p'         THEN 'Relaxation Massage 60min'
    WHEN 'Massage thư giãn 90p'         THEN 'Relaxation Massage 90min'
    WHEN 'Massage đầu & cổ'             THEN 'Head & Neck Massage'
    WHEN 'Massage bàn chân'             THEN 'Foot Massage'
    WHEN 'Massage đá nóng'              THEN 'Hot Stone Massage'
    WHEN 'Massage tinh dầu'             THEN 'Aromatherapy Massage'
    WHEN 'Chăm sóc da mặt cơ bản'      THEN 'Basic Facial'
    WHEN 'Chăm sóc da mặt chuyên sâu'  THEN 'Advanced Facial'
    WHEN 'Nặn mụn'                      THEN 'Acne Extraction'
    WHEN 'Đắp mặt nạ dưỡng ẩm'         THEN 'Hydrating Face Mask'
    WHEN 'Tẩy tế bào chết toàn thân'   THEN 'Full Body Exfoliation'
    WHEN 'Ủ trắng toàn thân'           THEN 'Full Body Whitening Treatment'
    WHEN 'Wax lông nách'               THEN 'Underarm Waxing'
    WHEN 'Wax lông chân'               THEN 'Leg Waxing'
    WHEN 'Wax bikini'                  THEN 'Bikini Waxing'
    WHEN 'Trị nám, tàn nhang'          THEN 'Pigmentation & Freckle Treatment'
    WHEN 'Trị mụn lưng'                THEN 'Back Acne Treatment'
    WHEN 'Combo mặt + massage'         THEN 'Facial + Massage Combo'
    WHEN 'Liệu trình 5 buổi'           THEN '5-Session Treatment Package'
    ELSE name_en END
WHERE name IN ('Massage thư giãn 60p','Massage thư giãn 90p','Massage đầu & cổ','Massage bàn chân',
               'Massage đá nóng','Massage tinh dầu','Chăm sóc da mặt cơ bản','Chăm sóc da mặt chuyên sâu',
               'Nặn mụn','Đắp mặt nạ dưỡng ẩm','Tẩy tế bào chết toàn thân','Ủ trắng toàn thân',
               'Wax lông nách','Wax lông chân','Wax bikini','Trị nám, tàn nhang','Trị mụn lưng',
               'Combo mặt + massage','Liệu trình 5 buổi');

-- BARBER_SHOP_MEN (V007 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Cắt tóc thường (nam)'          THEN 'Regular Haircut (Men)'
    WHEN 'Cắt Fade'                       THEN 'Fade Haircut'
    WHEN 'Cắt Undercut'                   THEN 'Undercut'
    WHEN 'Cắt tóc trẻ em (nam)'           THEN 'Boy''s Haircut'
    WHEN 'Cạo râu thường'                 THEN 'Standard Shave'
    WHEN 'Cạo râu + định hình râu'        THEN 'Shave + Beard Shaping'
    WHEN 'Trim & tỉa râu'                 THEN 'Beard Trim & Tidy'
    WHEN 'Gội đầu + massage đầu (nam)'    THEN 'Hair Wash + Head Massage (Men)'
    WHEN 'Massage đầu cổ vai 20p'         THEN 'Head, Neck & Shoulder Massage 20min'
    WHEN 'Tạo kiểu sáp / wax tóc'         THEN 'Hair Wax Styling'
    WHEN 'Nhuộm tóc nam'                  THEN 'Men''s Hair Coloring'
    WHEN 'Combo cắt + cạo râu'            THEN 'Haircut + Shave Combo'
    WHEN 'Combo cắt + gội + massage đầu'  THEN 'Haircut + Wash + Head Massage Combo'
    ELSE name_en END
WHERE name IN ('Cắt tóc thường (nam)','Cắt Fade','Cắt Undercut','Cắt tóc trẻ em (nam)',
               'Cạo râu thường','Cạo râu + định hình râu','Trim & tỉa râu',
               'Gội đầu + massage đầu (nam)','Massage đầu cổ vai 20p','Tạo kiểu sáp / wax tóc',
               'Nhuộm tóc nam','Combo cắt + cạo râu','Combo cắt + gội + massage đầu');

-- HAIR_SALON (V007 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Cắt tóc nữ ngắn'               THEN 'Women''s Short Haircut'
    WHEN 'Cắt tóc nữ dài'                THEN 'Women''s Long Haircut'
    WHEN 'Cắt tỉa layer'                  THEN 'Layer Cut'
    WHEN 'Nhuộm màu thời trang'           THEN 'Fashion Hair Coloring'
    WHEN 'Nhuộm highlight / ombre'        THEN 'Highlight / Ombre Coloring'
    WHEN 'Nhuộm phủ bạc'                  THEN 'Grey Coverage Coloring'
    WHEN 'Uốn xoăn Hàn Quốc'             THEN 'Korean-style Perm'
    WHEN 'Duỗi phồng / duỗi thẳng'       THEN 'Volume / Straightening Treatment'
    WHEN 'Ép tóc Keratin'                 THEN 'Keratin Treatment'
    WHEN 'Ủ phục hồi tóc hư tổn'          THEN 'Damaged Hair Repair Mask'
    WHEN 'Gội đầu dưỡng + massage đầu'    THEN 'Nourishing Hair Wash + Head Massage'
    WHEN 'Tạo kiểu đi tiệc / sự kiện'    THEN 'Event / Party Hairstyling'
    WHEN 'Combo cắt + nhuộm tóc'          THEN 'Cut + Color Combo'
    ELSE name_en END
WHERE name IN ('Cắt tóc nữ ngắn','Cắt tóc nữ dài','Cắt tỉa layer','Nhuộm màu thời trang',
               'Nhuộm highlight / ombre','Nhuộm phủ bạc','Uốn xoăn Hàn Quốc',
               'Duỗi phồng / duỗi thẳng','Ép tóc Keratin','Ủ phục hồi tóc hư tổn',
               'Gội đầu dưỡng + massage đầu','Tạo kiểu đi tiệc / sự kiện','Combo cắt + nhuộm tóc');

-- LASH_PMU_STUDIO (V007 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Nối mi cơ bản'               THEN 'Classic Lash Extensions'
    WHEN 'Nối mi volume'               THEN 'Volume Lash Extensions'
    WHEN 'Nối mi mega volume'          THEN 'Mega Volume Lash Extensions'
    WHEN 'Xăm mày tán bột / ombre'    THEN 'Powder Brow / Ombre Tattoo'
    WHEN 'Xăm mày giả lông'           THEN 'Hair-stroke Brow Tattoo'
    WHEN 'Xăm môi bóng / ombre'       THEN 'Glossy / Ombre Lip Tattoo'
    WHEN 'Xăm mí mắt trên'            THEN 'Upper Eyeliner Tattoo'
    WHEN 'Tháo mi'                    THEN 'Lash Removal'
    WHEN 'Điều chỉnh / fill mi'       THEN 'Lash Fill / Adjustment'
    WHEN 'Dưỡng phục hồi sau xăm'     THEN 'Post-Tattoo Recovery Care'
    WHEN 'Combo nối mi + fill mi'     THEN 'Lash Extension + Fill Combo'
    WHEN 'Combo mày + môi trọn gói'   THEN 'Brow + Lip Package'
    ELSE name_en END
WHERE name IN ('Nối mi cơ bản','Nối mi volume','Nối mi mega volume','Xăm mày tán bột / ombre',
               'Xăm mày giả lông','Xăm môi bóng / ombre','Xăm mí mắt trên','Tháo mi',
               'Điều chỉnh / fill mi','Dưỡng phục hồi sau xăm','Combo nối mi + fill mi',
               'Combo mày + môi trọn gói');

-- MASSAGE_SHOP (V007 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Massage thư giãn toàn thân 60p' THEN 'Full Body Relaxation Massage 60min'
    WHEN 'Massage toàn thân 90p'           THEN 'Full Body Massage 90min'
    WHEN 'Massage toàn thân 120p'          THEN 'Full Body Massage 120min'
    WHEN 'Massage chân phản xạ 30p'        THEN 'Foot Reflexology 30min'
    WHEN 'Massage chân phản xạ 60p'        THEN 'Foot Reflexology 60min'
    WHEN 'Massage đầu vai gáy 30p'         THEN 'Head, Neck & Shoulder Massage 30min'
    WHEN 'Massage lưng & cổ 30p'           THEN 'Back & Neck Massage 30min'
    WHEN 'Xông hơi ướt'                   THEN 'Steam Bath'
    WHEN 'Ngâm chân thảo dược'            THEN 'Herbal Foot Soak'
    WHEN 'Combo massage + ngâm chân'      THEN 'Massage + Foot Soak Combo'
    WHEN 'Combo toàn thân + xông hơi'     THEN 'Full Body Massage + Steam Combo'
    ELSE name_en END
WHERE name IN ('Massage thư giãn toàn thân 60p','Massage toàn thân 90p','Massage toàn thân 120p',
               'Massage chân phản xạ 30p','Massage chân phản xạ 60p','Massage đầu vai gáy 30p',
               'Massage lưng & cổ 30p','Xông hơi ướt','Ngâm chân thảo dược',
               'Combo massage + ngâm chân','Combo toàn thân + xông hơi');

-- BEAUTY_CLINIC (V007 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Nặn mụn an toàn tại thẩm mỹ viện' THEN 'Professional Acne Extraction'
    WHEN 'Trị mụn bằng laser'                THEN 'Laser Acne Treatment'
    WHEN 'Laser trẻ hóa da'                  THEN 'Laser Skin Rejuvenation'
    WHEN 'RF nâng cơ / căng da'              THEN 'RF Skin Tightening / Lifting'
    WHEN 'HIFU nâng cơ không phẫu thuật'     THEN 'HIFU Non-surgical Facelift'
    WHEN 'Triệt lông laser (1 vùng)'         THEN 'Laser Hair Removal (1 area)'
    WHEN 'Combo liệu trình da 5 buổi'        THEN '5-Session Skin Treatment Package'
    ELSE name_en END
WHERE name IN ('Nặn mụn an toàn tại thẩm mỹ viện','Trị mụn bằng laser','Laser trẻ hóa da',
               'RF nâng cơ / căng da','HIFU nâng cơ không phẫu thuật',
               'Triệt lông laser (1 vùng)','Combo liệu trình da 5 buổi');

-- MAKEUP_STUDIO (V007 rows)
UPDATE product_suggestions SET name_en = CASE name
    WHEN 'Trang điểm nhẹ nhàng hàng ngày' THEN 'Everyday Natural Makeup'
    WHEN 'Trang điểm Hàn Quốc (K-makeup)' THEN 'K-Beauty Makeup'
    WHEN 'Trang điểm đi tiệc ban ngày'    THEN 'Daytime Party Makeup'
    WHEN 'Trang điểm dự tiệc tối / event' THEN 'Evening / Event Makeup'
    WHEN 'Trang điểm tốt nghiệp'          THEN 'Graduation Makeup'
    WHEN 'Trang điểm chụp ảnh'            THEN 'Photo Shoot Makeup'
    WHEN 'Trang điểm cô dâu thử (trial)'  THEN 'Bridal Trial Makeup'
    WHEN 'Trang điểm cô dâu ngày cưới'   THEN 'Wedding Day Bridal Makeup'
    WHEN 'Trang điểm phụ dâu / phù rể'   THEN 'Bridesmaid / Groomsman Makeup'
    WHEN 'Búi tóc đơn giản'               THEN 'Simple Hair Updo'
    WHEN 'Tạo kiểu tóc đi tiệc / sự kiện' THEN 'Event / Party Hairstyling'
    WHEN 'Combo trang điểm + tóc tiệc'   THEN 'Party Makeup + Hair Combo'
    WHEN 'Gói cưới cô dâu cơ bản'        THEN 'Basic Bridal Wedding Package'
    ELSE name_en END
WHERE name IN ('Trang điểm nhẹ nhàng hàng ngày','Trang điểm Hàn Quốc (K-makeup)',
               'Trang điểm đi tiệc ban ngày','Trang điểm dự tiệc tối / event',
               'Trang điểm tốt nghiệp','Trang điểm chụp ảnh','Trang điểm cô dâu thử (trial)',
               'Trang điểm cô dâu ngày cưới','Trang điểm phụ dâu / phù rể','Búi tóc đơn giản',
               'Tạo kiểu tóc đi tiệc / sự kiện','Combo trang điểm + tóc tiệc','Gói cưới cô dâu cơ bản');

-- expense_suggestions: English translations
UPDATE expense_suggestions SET name_en = CASE name
    WHEN 'Tiền thuê mặt bằng'            THEN 'Rent'
    WHEN 'Tiền điện'                      THEN 'Electricity'
    WHEN 'Tiền nước'                      THEN 'Water'
    WHEN 'Internet / WiFi'               THEN 'Internet / WiFi'
    WHEN 'Tiền điện thoại'               THEN 'Phone Bill'
    WHEN 'Lương nhân viên'               THEN 'Staff Wages'
    WHEN 'Vệ sinh cửa hàng'              THEN 'Shop Cleaning'
    WHEN 'Sửa chữa / bảo trì'           THEN 'Repair / Maintenance'
    WHEN 'Phí phần mềm quản lý'          THEN 'Management Software Fee'
    WHEN 'Chi phí quảng cáo / fanpage'   THEN 'Advertising / Social Media'
    WHEN 'Phí ngân hàng / chuyển khoản'  THEN 'Bank / Transfer Fees'
    WHEN 'Bảo hiểm cửa hàng'             THEN 'Shop Insurance'
    WHEN 'Thuế môn bài / phí kinh doanh' THEN 'Business License Tax'
    WHEN 'Camera / thiết bị an ninh'     THEN 'Security Camera / Equipment'
    WHEN 'In ấn / văn phòng phẩm'        THEN 'Printing / Stationery'
    WHEN 'Trang trí / nội thất cửa hàng' THEN 'Shop Decoration / Furniture'
    WHEN 'Đồng phục nhân viên'           THEN 'Staff Uniforms'
    WHEN 'Ăn uống nhân viên'             THEN 'Staff Meals'
    WHEN 'Chi phí giao hàng'             THEN 'Delivery Costs'
    WHEN 'Bao bì / túi đựng'             THEN 'Packaging / Bags'
    ELSE name_en END
WHERE name IN ('Tiền thuê mặt bằng','Tiền điện','Tiền nước','Internet / WiFi','Tiền điện thoại',
               'Lương nhân viên','Vệ sinh cửa hàng','Sửa chữa / bảo trì','Phí phần mềm quản lý',
               'Chi phí quảng cáo / fanpage','Phí ngân hàng / chuyển khoản','Bảo hiểm cửa hàng',
               'Thuế môn bài / phí kinh doanh','Camera / thiết bị an ninh','In ấn / văn phòng phẩm',
               'Trang trí / nội thất cửa hàng','Đồng phục nhân viên','Ăn uống nhân viên',
               'Chi phí giao hàng','Bao bì / túi đựng');

UPDATE expense_suggestions SET name_en = CASE name
    WHEN 'Nguyên liệu / thực phẩm'                  THEN 'Ingredients / Food Supplies'
    WHEN 'Gas / nhiên liệu nấu ăn'                   THEN 'Gas / Cooking Fuel'
    WHEN 'Dụng cụ bếp / nhà hàng'                   THEN 'Kitchen Equipment'
    WHEN 'Nguyên liệu cà phê / trà'                  THEN 'Coffee / Tea Ingredients'
    WHEN 'Ly / cốc / đồ pha chế'                    THEN 'Cups / Glasses / Barware'
    WHEN 'Phí hoa hồng ứng dụng giao đồ ăn'         THEN 'Food Delivery App Commission'
    WHEN 'Tủ lạnh bảo quản thuốc'                   THEN 'Medicine Refrigerator'
    WHEN 'Phí kiểm định / giấy phép dược phẩm'      THEN 'Pharmacy License / Inspection Fee'
    WHEN 'Bao bì đóng gói thuốc'                    THEN 'Medicine Packaging'
    WHEN 'Phí sàn thương mại điện tử'               THEN 'E-commerce Platform Fee'
    WHEN 'Móc treo / giá trưng bày'                 THEN 'Display Racks / Hangers'
    WHEN 'Bao bì / túi thời trang'                  THEN 'Fashion Bags / Packaging'
    WHEN 'Linh kiện / phụ kiện thay thế'            THEN 'Replacement Parts / Accessories'
    WHEN 'Chi phí bảo hành / dịch vụ sau bán'       THEN 'Warranty / After-sales Service Cost'
    WHEN 'Chi phí giám định hàng hóa'               THEN 'Product Authentication Cost'
    WHEN 'Két sắt / thiết bị bảo mật'               THEN 'Safe / Security Equipment'
    WHEN 'Phí bảo hiểm hàng quý giá'                THEN 'Valuable Goods Insurance'
    WHEN 'Vật tư dịch vụ (dao, kéo, hóa chất)'     THEN 'Service Supplies (razors, scissors, chemicals)'
    WHEN 'Khăn / đồ vệ sinh cá nhân'                THEN 'Towels / Personal Hygiene Supplies'
    WHEN 'Bảo trì / thuê ghế cắt tóc'               THEN 'Barber Chair Maintenance / Rental'
    WHEN 'Phí kiểm kho / kiểm đếm hàng'             THEN 'Stock Count / Inventory Fee'
    WHEN 'Túi nilon / bao bì siêu thị'              THEN 'Plastic Bags / Supermarket Packaging'
    WHEN 'Sơn / gel / bột nail'                     THEN 'Nail Polish / Gel / Powder'
    WHEN 'Đèn UV / máy khoan nail'                  THEN 'UV Lamp / Nail Drill'
    WHEN 'Khăn / bông tẩy trang / phụ kiện'         THEN 'Towels / Cotton Pads / Accessories'
    WHEN 'Dầu massage / tinh dầu aromatherapy'       THEN 'Massage Oil / Aromatherapy Essential Oil'
    WHEN 'Kem dưỡng / mặt nạ / vật tư spa'          THEN 'Moisturiser / Mask / Spa Supplies'
    WHEN 'Khăn / đồ vải spa'                        THEN 'Towels / Spa Linen'
    WHEN 'Máy massage / thiết bị spa'                THEN 'Massage Machine / Spa Equipment'
    WHEN 'Vật tư cắt tóc (tông đơ, dao, kéo)'       THEN 'Barbering Supplies (clippers, razors, scissors)'
    WHEN 'Dầu cạo râu / kem cạo râu'                THEN 'Shaving Oil / Shaving Cream'
    WHEN 'Khăn bông / khăn lạnh phục vụ'            THEN 'Cotton Towels / Cold Towels'
    WHEN 'Bảo trì ghế cắt / thiết bị salon'         THEN 'Barber Chair / Equipment Maintenance'
    WHEN 'Thuốc nhuộm / thuốc uốn / hóa chất tóc'  THEN 'Hair Dye / Perm / Hair Chemicals'
    WHEN 'Dầu gội / dầu xả chuyên nghiệp'           THEN 'Professional Shampoo / Conditioner'
    WHEN 'Khăn bông / áo choàng khách'              THEN 'Cotton Towels / Client Gowns'
    WHEN 'Bảo trì máy sấy / máy uốn / máy duỗi'    THEN 'Hair Dryer / Curler / Straightener Maintenance'
    WHEN 'Chỉ mi / keo mi / dung môi tháo keo'      THEN 'Lash Thread / Adhesive / Remover'
    WHEN 'Mực xăm / kim xăm tiêu hao'               THEN 'PMU Ink / Needles'
    WHEN 'Khăn vô trùng / vật tư tiệt khuẩn'        THEN 'Sterile Towels / Sterilisation Supplies'
    WHEN 'Bảo trì giường / ghế kỹ thuật viên'       THEN 'Technician Bed / Chair Maintenance'
    WHEN 'Tinh dầu / dầu massage chuyên dụng'       THEN 'Essential Oil / Professional Massage Oil'
    WHEN 'Khăn bông / đồ vải massage'               THEN 'Cotton Towels / Massage Linen'
    WHEN 'Bảo trì giường massage'                   THEN 'Massage Bed Maintenance'
    WHEN 'Đá bazan / thiết bị nhiệt massage'        THEN 'Basalt Stones / Thermal Massage Equipment'
    WHEN 'Hóa chất / serum / ampoule điều trị'      THEN 'Treatment Chemicals / Serum / Ampoule'
    WHEN 'Kim vi kim / đầu mũi khoan tiêu hao'      THEN 'Microneedles / Drill Tips'
    WHEN 'Khăn vô trùng / vật tư y tế 1 lần'        THEN 'Sterile Towels / Single-use Medical Supplies'
    WHEN 'Bảo trì thiết bị laser / RF / HIFU'       THEN 'Laser / RF / HIFU Equipment Maintenance'
    WHEN 'Phí kiểm định thiết bị thẩm mỹ'           THEN 'Aesthetic Equipment Inspection Fee'
    WHEN 'Mỹ phẩm / son / phấn / kem nền'           THEN 'Cosmetics / Lipstick / Powder / Foundation'
    WHEN 'Cọ makeup / dụng cụ trang điểm'           THEN 'Makeup Brushes / Tools'
    WHEN 'Đèn ring light / ghế trang điểm'          THEN 'Ring Light / Makeup Chair'
    WHEN 'Áo choàng / khăn phục vụ khách'           THEN 'Client Gown / Towels'
    ELSE name_en END
WHERE name_en IS NULL;

-- ════════════════════════════════════════════════════════════
-- Merged from: V009__add_table_service.sql
-- ════════════════════════════════════════════════════════════

-- TABLE_SERVICE feature (id=202601039)
INSERT INTO features (id, name, display_name, description, active, deleted)
VALUES (202601039, 'TABLE_SERVICE', 'Quản Lý Bàn', 'Theo dõi trạng thái bàn và gọi món theo bàn cho quán ăn / quán nhậu', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('features', 'id'), 202601039, true);

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

-- ════════════════════════════════════════════════════════════
-- Merged from: V010__add_pub_product_suggestions.sql
-- ════════════════════════════════════════════════════════════

INSERT INTO product_suggestions
    (name, name_en, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, category_name, display_order)
VALUES
-- ── Shared pub beverages ──────────────────────────────────────────────────
('Bia Saigon Special',   'Saigon Special Beer',    '🍺', 20000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        1),
('Bia Tiger Crystal',    'Tiger Crystal Beer',     '🍺', 22000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        2),
('Bia Heineken',         'Heineken Beer',          '🍺', 25000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        3),
('Bia 333',              '333 Beer',               '🍺', 18000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        4),
('Két bia Saigon',       'Case of Saigon Beer',    '🍺', 400000, 'Két',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        5),
('Rượu đế / rượu gạo',  'Rice Wine',              '🥃', 50000,  'Chai', 'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        6),
('Nước ngọt (lon)',      'Soft Drink (can)',       '🥤', 12000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        7),
('Nước suối',            'Water',                  '💧', 5000,   'Chai', 'BEVERAGE', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Bia & Rượu',        8),

-- ── General pub snacks / mồi ─────────────────────────────────────────────
('Đậu phộng rang muối',  'Salted Roasted Peanuts', '🥜', 30000,  'Đĩa', 'FOOD', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Đồ nhậu',            9),
('Khô mực nướng',        'Grilled Dried Squid',    '🦑', 80000,  'Đĩa', 'FOOD', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Đồ nhậu',            10),
('Hột vịt lộn',          'Balut Eggs',             '🥚', 15000,  'Trứng','FOOD', FALSE, ARRAY['PUB','PUB_SEAFOOD','PUB_GOAT','PUB_BEEF'], 'Đồ nhậu',            11),
('Gà nướng muối ớt',     'Salt & Chili Grilled Chicken', '🍗', 180000, 'Con', 'FOOD', FALSE, ARRAY['PUB'], 'Đồ nhậu',             12),

-- ── PUB_SEAFOOD — hải sản ────────────────────────────────────────────────
('Tôm sú nướng muối ớt', 'Grilled Tiger Prawns',   '🦐', 250000, 'Kg',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  1),
('Cua rang muối',         'Salt & Pepper Crab',     '🦀', 350000, 'Con', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  2),
('Mực chiên giòn',        'Crispy Fried Squid',     '🦑', 180000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  3),
('Nghêu hấp sả',          'Steamed Clams with Lemongrass', '🐚', 120000, 'Kg', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống', 4),
('Bạch tuộc nướng',       'Grilled Octopus',        '🐙', 220000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  5),
('Cá lóc nướng trui',     'Grilled Snakehead Fish', '🐟', 200000, 'Con', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  6),
('Ghẹ hấp bia',           'Beer-steamed Blue Crab', '🦀', 280000, 'Con', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Hải sản tươi sống',  7),
('Lẩu hải sản thập cẩm',  'Mixed Seafood Hot Pot',  '🫕', 350000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Lẩu hải sản',        8),
('Lẩu tôm cua',           'Prawn & Crab Hot Pot',   '🫕', 420000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 'Lẩu hải sản',        9),

-- ── PUB_GOAT — thịt dê ───────────────────────────────────────────────────
('Thịt dê xào lăn',      'Sautéed Goat with Lemongrass', '🐐', 200000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',           1),
('Dê nướng nguyên con',  'Whole Roasted Goat',     '🐐', 1500000,'Con', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              2),
('Dê nướng bếp than',    'Charcoal Grilled Goat',  '🔥', 250000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              3),
('Lẩu dê',               'Goat Hot Pot',           '🫕', 350000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              4),
('Tiết canh dê',         'Goat Blood Pudding',     '🍲', 80000,  'Bát', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              5),
('Dê hấp gừng',          'Steamed Goat with Ginger','🐐', 220000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',             6),
('Dồi dê nướng',         'Grilled Goat Sausage',   '🌭', 150000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              7),
('Dê sốt vang',          'Goat in Red Wine Sauce', '🍷', 200000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_GOAT'], 'Thịt dê',              8),

-- ── PUB_BEEF — thịt bò ───────────────────────────────────────────────────
('Bò nhúng dấm',         'Beef Dipped in Vinegar', '🥢', 280000, 'Phần','FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              1),
('Bò nướng ngũ vị',      'Five-Spice Grilled Beef','🔥', 250000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              2),
('Lẩu bò',               'Beef Hot Pot',           '🫕', 320000, 'Nồi', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              3),
('Bắp bò kho gừng',      'Braised Beef with Ginger','🍲', 180000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',             4),
('Gân bò hầm',           'Braised Beef Tendon',    '🍖', 150000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              5),
('Bò tái chanh',         'Rare Beef with Lime',    '🍋', 170000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              6),
('Bò nướng lá lốt',      'Beef Wrapped in Betel Leaf','🌿', 180000, 'Đĩa', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',            7),
('Bò kho bánh mì',       'Beef Stew with Bread',   '🥖', 120000, 'Bát', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 'Thịt bò',              8)
ON CONFLICT (name) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V002: Add soft delete columns to salary tables
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE salary
    ADD COLUMN IF NOT EXISTS deleted    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

ALTER TABLE salary_advance
    ADD COLUMN IF NOT EXISTS deleted    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V003: Add description and logo_url to shop_info
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE shop_info
    ADD COLUMN IF NOT EXISTS description TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS logo_url    VARCHAR(500) DEFAULT NULL;

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V004: Add walk_in flag to customers
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE customers ADD COLUMN IF NOT EXISTS walk_in BOOLEAN NOT NULL DEFAULT FALSE;

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V005: Create default_expense table
-- ══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS default_expense (
    id            BIGSERIAL      PRIMARY KEY,
    tenant_id     VARCHAR(100)   NOT NULL,
    description   VARCHAR(500)   NOT NULL,
    amount        DECIMAL(20,0)  NOT NULL DEFAULT 0,
    category      VARCHAR(30)    NOT NULL DEFAULT 'OTHER',
    payment_day   SMALLINT       DEFAULT NULL CHECK (payment_day BETWEEN 1 AND 31),
    display_order INT            NOT NULL DEFAULT 0,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      DEFAULT NOW(),
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP      DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_default_expense_tenant ON default_expense (tenant_id);

ALTER TABLE default_expense ENABLE ROW LEVEL SECURITY;
ALTER TABLE default_expense FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON default_expense
    USING (tenant_id = current_setting('app.current_tenant', TRUE));

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V006: Add nick_name to employees, make position nullable
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS nick_name VARCHAR(100);

ALTER TABLE employees
    ALTER COLUMN position DROP NOT NULL;

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V007: Trim nickname length to VARCHAR(20)
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE users
    ALTER COLUMN nickname TYPE VARCHAR(20);

ALTER TABLE employees
    ALTER COLUMN nick_name TYPE VARCHAR(20);

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V009: Add monthly order limit to tenants
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_orders_per_month INT DEFAULT NULL;

UPDATE tenants SET max_orders_per_month = 1000
WHERE subscription_type IN ('TRIAL', 'STARTER') OR subscription_type IS NULL;

UPDATE tenants SET max_orders_per_month = 5000
WHERE subscription_type = 'BASIC';

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V010: Make expiration_date NOT NULL with default 1 year
-- ══════════════════════════════════════════════════════════════════════════════
UPDATE tenants SET expiration_date = CURRENT_DATE + INTERVAL '1 year' WHERE expiration_date IS NULL;

ALTER TABLE tenants ALTER COLUMN expiration_date SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN expiration_date SET DEFAULT (CURRENT_DATE + INTERVAL '1 year');

-- ══════════════════════════════════════════════════════════════════════════════
-- Merged from V008: Expand product_suggestions table
-- ══════════════════════════════════════════════════════════════════════════════
-- ============================================================
-- V008: Expand product_suggestions table
-- - Add duration_minutes column
-- - Fix BARBER_SHOP entries (prices + duration + product_type_code)
-- - Insert missing BARBER_SHOP products
-- - Insert all products from every tenant DML file
-- ============================================================

-- ── Step A: Add duration_minutes column ──────────────────────
ALTER TABLE product_suggestions ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 0;

-- ── Step B: Fix existing BARBER_SHOP entries ─────────────────
-- Update product_type_code from 'BEAUTY' to 'SERVICE' and fix prices + duration
UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 80000,
    duration_minutes  = 30
WHERE name = 'Cắt tóc nam'    AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 120000,
    duration_minutes  = 45
WHERE name = 'Cắt tóc nữ'    AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 60000,
    duration_minutes  = 20
WHERE name = 'Cắt tóc trẻ em' AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 400000,
    duration_minutes  = 90
WHERE name = 'Nhuộm tóc'      AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 500000,
    duration_minutes  = 120
WHERE name = 'Uốn tóc'        AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 500000,
    duration_minutes  = 120
WHERE name = 'Duỗi tóc'       AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 80000,
    duration_minutes  = 30
WHERE name = 'Gội đầu'        AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 50000,
    duration_minutes  = 15
WHERE name = 'Cạo râu'        AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 100000,
    duration_minutes  = 30
WHERE name = 'Massage đầu'    AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 200000,
    duration_minutes  = 60
WHERE name = 'Phục hồi tóc'   AND 'BARBER_SHOP' = ANY(shop_types);

UPDATE product_suggestions SET
    product_type_code = 'SERVICE',
    default_price     = 100000,
    duration_minutes  = 30
WHERE name = 'Tạo kiểu tóc'   AND 'BARBER_SHOP' = ANY(shop_types);

-- ── Step C: Insert missing BARBER_SHOP products ───────────────
-- (entries not already in product_suggestions from barber_shop.sql)
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Cắt + gội đầu nam',            '💇', 100000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 121, 'Cắt tóc nam',       45),
('Cắt Fade / Undercut',           '💇', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 122, 'Cắt tóc nam',       60),
('Cắt + gội đầu nữ',             '💇', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 123, 'Cắt tóc nữ',        60),
('Cắt layer / Cắt tỉa nữ',       '💇', 150000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 124, 'Cắt tóc nữ',        60),
('Nhuộm highlight',               '💈', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 125, 'Nhuộm & Uốn',      120),
('Ép tóc Keratin',                '💈', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 126, 'Nhuộm & Uốn',      180),
('Tỉa râu + tạo hình',           '🪒', 80000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 127, 'Chăm sóc râu',      30),
('Cạo râu nóng truyền thống',     '🪒', 70000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 128, 'Chăm sóc râu',      20),
('Gội + massage đầu',             '💆', 130000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 129, 'Gội đầu & Massage', 45),
('Hấp dầu phục hồi tóc',         '💇', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 130, 'Gội đầu & Massage', 60),
('Tạo kiểu đặc biệt',            '💇', 250000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 131, 'Tạo kiểu & Combo',  60),
('Combo cắt + cạo râu (barber)',  '💈', 120000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 132, 'Tạo kiểu & Combo',  45),
('Combo đầy đủ (barber)',         '💈', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 133, 'Tạo kiểu & Combo',  90),
('Gói chăm sóc tóc (barber)',    '💈', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 134, 'Tạo kiểu & Combo', 150),
('Combo trẻ em (barber)',         '👦', 80000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP'], 135, 'Tạo kiểu & Combo',  30)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ── Step D: All other shop types ──────────────────────────────

-- ─ BARBER_SHOP_MEN: update existing + add new entries (display_order 400+) ─
-- Existing entries from V007 already have 400-441; update their duration
UPDATE product_suggestions SET duration_minutes = 25 WHERE name = 'Cắt tóc thường (nam)';
UPDATE product_suggestions SET duration_minutes = 45 WHERE name = 'Cắt Fade';
UPDATE product_suggestions SET duration_minutes = 45 WHERE name = 'Cắt Undercut';
UPDATE product_suggestions SET duration_minutes = 20 WHERE name = 'Cắt tóc trẻ em (nam)';
UPDATE product_suggestions SET duration_minutes = 15 WHERE name = 'Cạo râu thường';
UPDATE product_suggestions SET duration_minutes = 25 WHERE name = 'Cạo râu + định hình râu';
UPDATE product_suggestions SET duration_minutes = 20 WHERE name = 'Trim & tỉa râu';
UPDATE product_suggestions SET duration_minutes = 25 WHERE name = 'Gội đầu + massage đầu (nam)';
UPDATE product_suggestions SET duration_minutes = 20 WHERE name = 'Massage đầu cổ vai 20p';
UPDATE product_suggestions SET duration_minutes = 15 WHERE name = 'Tạo kiểu sáp / wax tóc';
UPDATE product_suggestions SET duration_minutes = 60 WHERE name = 'Nhuộm tóc nam';
UPDATE product_suggestions SET duration_minutes = 40 WHERE name = 'Combo cắt + cạo râu';
UPDATE product_suggestions SET duration_minutes = 60 WHERE name = 'Combo cắt + gội + massage đầu';

-- Missing barber_shop_men entries
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Cắt Buzz Cut',                          '💇', 70000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 404, 'Cắt tóc',              20),
('Cắt kỹ thuật cao (Textured/Taper)',      '💇', 180000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 405, 'Cắt tóc',              60),
('Cạo râu nóng truyền thống (men)',        '🪒', 80000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 413, 'Cạo & Chăm sóc râu',  25),
('Chăm sóc râu cao cấp',                  '🪒', 120000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 414, 'Cạo & Chăm sóc râu',  40),
('Gội đầu dưỡng tóc (nam)',               '💆', 70000,   'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 422, 'Gội đầu & Massage',    25),
('Combo Full Barber',                      '💈', 280000,  'Lần', 'SERVICE', FALSE, ARRAY['BARBER_SHOP_MEN'], 442, 'Combo',                90)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ HAIR_SALON: update durations for existing + add missing entries ─
UPDATE product_suggestions SET duration_minutes = 45 WHERE name = 'Cắt tóc nữ ngắn';
UPDATE product_suggestions SET duration_minutes = 60 WHERE name = 'Cắt tóc nữ dài';
UPDATE product_suggestions SET duration_minutes = 60 WHERE name = 'Cắt tỉa layer';
UPDATE product_suggestions SET duration_minutes = 90 WHERE name = 'Nhuộm màu thời trang';
UPDATE product_suggestions SET duration_minutes = 150 WHERE name = 'Nhuộm highlight / ombre';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Nhuộm phủ bạc';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Uốn xoăn Hàn Quốc';
UPDATE product_suggestions SET duration_minutes = 150 WHERE name = 'Duỗi phồng / duỗi thẳng';
UPDATE product_suggestions SET duration_minutes = 180 WHERE name = 'Ép tóc Keratin';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Ủ phục hồi tóc hư tổn';
UPDATE product_suggestions SET duration_minutes = 40  WHERE name = 'Gội đầu dưỡng + massage đầu';
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Tạo kiểu đi tiệc / sự kiện';
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'Combo cắt + nhuộm tóc';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Cắt tóc nam / unisex',              '✂️', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 453, 'Cắt tóc',           30),
('Tỉa tóc & chỉnh đuôi',             '✂️', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 454, 'Cắt tóc',           30),
('Nhuộm màu toàn bộ (tóc dài)',       '🎨', 600000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 463, 'Nhuộm tóc',        120),
('Tẩy tóc',                           '🎨', 400000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 464, 'Nhuộm tóc',         60),
('Uốn xoăn tóc dài',                  '💫', 600000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 473, 'Uốn & Duỗi',       150),
('Uốn phồng chân tóc',                '💫', 500000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 474, 'Uốn & Duỗi',       120),
('Hấp dầu Collagen',                  '🌿', 300000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 481, 'Chăm sóc tóc',      75),
('Cắt tỉa tóc hư tơi',               '✂️', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 482, 'Chăm sóc tóc',      20),
('Gội đầu đơn (salon)',               '💆', 60000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 491, 'Gội đầu & Massage', 25),
('Gội đầu + xả dưỡng',               '💆', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 492, 'Gội đầu & Massage', 35),
('Combo Cắt + Gội + Sấy',             '💈', 200000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 502, 'Tạo kiểu & Combo',  75),
('Combo Cắt + Hấp dầu (salon)',       '💈', 300000, 'Lần', 'SERVICE', FALSE, ARRAY['HAIR_SALON'], 503, 'Tạo kiểu & Combo',  90)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ LASH_PMU_STUDIO: update durations for existing + add missing ─
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'Nối mi cơ bản';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Nối mi volume';
UPDATE product_suggestions SET duration_minutes = 150 WHERE name = 'Nối mi mega volume';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Xăm mày tán bột / ombre';
UPDATE product_suggestions SET duration_minutes = 150 WHERE name = 'Xăm mày giả lông';
UPDATE product_suggestions SET duration_minutes = 150 WHERE name = 'Xăm môi bóng / ombre';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Xăm mí mắt trên';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Tháo mi';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Điều chỉnh / fill mi';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Dưỡng phục hồi sau xăm';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Combo nối mi + fill mi';
UPDATE product_suggestions SET duration_minutes = 240 WHERE name = 'Combo mày + môi trọn gói';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Nối mi Hybrid',                     '👁', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 513, 'Nối mi',          120),
('Nối mi Wispy',                      '👁', 650000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 514, 'Nối mi',          130),
('Nối mi Cat-eye',                    '👁', 550000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 515, 'Nối mi',          110),
('Xăm mày ngang cơ bản',             '✏', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 522, 'Xăm mày',          90),
('Xăm mày lông tơ Nano',             '✏', 2000000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 523, 'Xăm mày',         150),
('Điều chỉnh / Sửa màu mày',        '✏', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 524, 'Xăm mày',          60),
('Xăm môi tán viền',                 '💋', 1300000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 531, 'Xăm môi',         120),
('Điều chỉnh / Sửa màu môi',        '💋', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 532, 'Xăm môi',          60),
('Xăm mí trên (liner đậm)',          '👁', 1000000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 541, 'Xăm mí mắt',       75),
('Điền mi (fill-in)',                 '👁', 250000,  'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 553, 'Chăm sóc & Tháo',  60),
('Combo Nối mi + Xăm mày',          '✨', 1400000, 'Lần', 'SERVICE', FALSE, ARRAY['LASH_PMU_STUDIO'], 562, 'Combo',            180)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ MASSAGE_SHOP: update durations for existing + add missing ─
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Massage thư giãn toàn thân 60p';
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'Massage toàn thân 90p';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Massage toàn thân 120p';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Massage chân phản xạ 30p';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Massage chân phản xạ 60p';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Massage đầu vai gáy 30p';
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Massage lưng & cổ 30p';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Xông hơi ướt';
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Ngâm chân thảo dược';
UPDATE product_suggestions SET duration_minutes = 75  WHERE name = 'Combo massage + ngâm chân';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Combo toàn thân + xông hơi';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Massage aroma toàn thân',           '🌿', 350000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 573, 'Massage toàn thân',     60),
('Massage đá nóng toàn thân',         '🪨', 450000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 574, 'Massage toàn thân',     75),
('Massage dầu nóng toàn thân',        '🌿', 300000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 575, 'Massage toàn thân',     60),
('Massage chân phản xạ 45p',          '🦶', 150000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 582, 'Massage chân phản xạ',  45),
('Ngâm chân + massage chân',         '🌿', 180000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 583, 'Massage chân phản xạ',  45),
('Massage đầu 20p',                   '💆', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 591, 'Massage đầu & vai gáy', 20),
('Massage đầu & vai gáy 45p',         '💆', 150000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 592, 'Massage đầu & vai gáy', 45),
('Massage lưng & cổ 45p',             '💆', 150000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 601, 'Massage lưng & cổ',     45),
('Bấm huyệt lưng trị liệu',           '💆', 200000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 602, 'Massage lưng & cổ',     45),
('Xông hơi khô',                      '🌊', 80000,  'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 612, 'Xông hơi & Ngâm',       30),
('Ngâm bồn thảo dược',                '🌿', 150000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 613, 'Xông hơi & Ngâm',       30),
('Combo Full Relax (xông + massage)', '✨', 550000, 'Lần', 'SERVICE', FALSE, ARRAY['MASSAGE_SHOP'], 622, 'Combo',                 120)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ BEAUTY_CLINIC: update durations for shared entries + add missing ─
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Chăm sóc da mặt cơ bản';
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'Chăm sóc da mặt chuyên sâu';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Đắp mặt nạ dưỡng ẩm';
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Tẩy tế bào chết toàn thân';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Ủ trắng toàn thân';
UPDATE product_suggestions SET duration_minutes = 15  WHERE name = 'Wax lông nách';
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Wax lông chân';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Trị nám, tàn nhang';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Nặn mụn an toàn tại thẩm mỹ viện';
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'Trị mụn bằng laser';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Laser trẻ hóa da';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'RF nâng cơ / căng da';
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'HIFU nâng cơ không phẫu thuật';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Triệt lông laser (1 vùng)';
UPDATE product_suggestions SET duration_minutes = 300 WHERE name = 'Combo liệu trình da 5 buổi';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Lột da hóa học (Chemical Peel)',    '✨', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 632, 'Chăm sóc da mặt',      60),
('Nặn mụn chuyên nghiệp (clinic)',   '🫧', 350000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 633, 'Chăm sóc da mặt',      60),
('Trị nám laser',                     '💡', 1500000, 'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 634, 'Trị mụn & Nám',        45),
('Trị thâm sau mụn',                 '✨', 500000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 635, 'Trị mụn & Nám',        60),
('Microneedling tái tạo da',          '💉', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 643, 'Công nghệ thẩm mỹ',    60),
('IPL trị sắc tố',                   '💡', 1000000, 'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 644, 'Công nghệ thẩm mỹ',    45),
('LED Therapy ánh sáng sinh học',     '💡', 400000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 645, 'Công nghệ thẩm mỹ',    30),
('Ủ dưỡng thể trắng da (clinic)',    '🧖', 450000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 651, 'Điều trị cơ thể',      60),
('Điều trị rạn da',                  '💉', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 652, 'Điều trị cơ thể',      60),
('Triệt lông laser (vùng lớn)',       '💡', 1500000, 'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 653, 'Waxing & Triệt lông',  60),
('Combo Trị mụn + Chăm sóc da',     '✨', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['BEAUTY_CLINIC'], 661, 'Combo & Liệu trình',  120)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ MAKEUP_STUDIO: update durations for existing + add missing ─
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Trang điểm nhẹ nhàng hàng ngày';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Trang điểm Hàn Quốc (K-makeup)';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Trang điểm đi tiệc ban ngày';
UPDATE product_suggestions SET duration_minutes = 75  WHERE name = 'Trang điểm dự tiệc tối / event';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Trang điểm tốt nghiệp';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Trang điểm chụp ảnh';
UPDATE product_suggestions SET duration_minutes = 90  WHERE name = 'Trang điểm cô dâu thử (trial)';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Trang điểm cô dâu ngày cưới';
UPDATE product_suggestions SET duration_minutes = 60  WHERE name = 'Trang điểm phụ dâu / phù rể';
UPDATE product_suggestions SET duration_minutes = 30  WHERE name = 'Búi tóc đơn giản';
UPDATE product_suggestions SET duration_minutes = 45  WHERE name = 'Tạo kiểu tóc đi tiệc / sự kiện';
UPDATE product_suggestions SET duration_minutes = 120 WHERE name = 'Combo trang điểm + tóc tiệc';
UPDATE product_suggestions SET duration_minutes = 360 WHERE name = 'Gói cưới cô dâu cơ bản';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Trang điểm retouching (chỉnh sửa)','💄', 100000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 672, 'Trang điểm ngày thường',  20),
('Trang điểm Halloween / cosplay',   '🎭', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 684, 'Trang điểm đi tiệc',      90),
('Trang điểm cô dâu cao cấp',        '👰', 2500000, 'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 693, 'Trang điểm cô dâu',      150),
('Trang điểm mẹ cô dâu / chú rể',   '💍', 600000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 694, 'Trang điểm cô dâu',       75),
('Tạo kiểu tóc cô dâu',              '💇', 800000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 702, 'Làm tóc & Phụ kiện',      90),
('Đặt vương miện / phụ kiện tóc',   '👑', 200000,  'Lần', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 703, 'Làm tóc & Phụ kiện',      20),
('Gói cưới cô dâu cao cấp',          '👰', 4500000, 'Gói', 'SERVICE', FALSE, ARRAY['MAKEUP_STUDIO'], 712, 'Combo & Gói cưới',       480)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ NAIL_SHOP: fix product_type_code + add duration_minutes ───
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 30 WHERE name = 'Sơn màu thường (tay)';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 25 WHERE name = 'Sơn màu thường (chân)';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 35 WHERE name = 'Sơn French';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 50 WHERE name = 'Sơn gel (tay)';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 40 WHERE name = 'Sơn gel (chân)';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 90 WHERE name = 'Đắp bột acrylic';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 80 WHERE name = 'Đắp gel builder';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 30 WHERE name = 'Tháo gel / Tháo bột';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 30 WHERE name = 'Vẽ nail';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 90 WHERE name = 'Nail art';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 20 WHERE name = 'Đính đá nail';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 70 WHERE name = 'Nail ombre';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 40 WHERE name = 'Manicure';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 25 WHERE name = 'Dưỡng ẩm tay';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 50 WHERE name = 'Pedicure';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 35 WHERE name = 'Tẩy da chết chân';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 50 WHERE name = 'Combo tay + chân';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 80 WHERE name = 'Combo gel tay + chân';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Sơn màu Pháp (French nail)',        '💅', 100000, 'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 203, 'Sơn móng thường',     35),
('Đắp bột Dip Powder',               '💅', 250000, 'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 215, 'Gel & Acrylic',        75),
('Vẽ nail đơn giản (mỗi ngón)',      '🎨', 20000,  'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 224, 'Vẽ nail & Nghệ thuật', 10),
('Vẽ nail phức tạp (mỗi ngón)',      '🎨', 40000,  'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 225, 'Vẽ nail & Nghệ thuật', 20),
('Nail art full set (tay)',           '🎨', 350000, 'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 226, 'Vẽ nail & Nghệ thuật', 90),
('Đính đá / Phụ kiện (mỗi ngón)',   '💎', 25000,  'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 227, 'Vẽ nail & Nghệ thuật', 10),
('Manicure + Sơn gel',               '✋', 220000, 'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 232, 'Chăm sóc bàn tay',    70),
('Pedicure + Sơn gel',               '🦶', 230000, 'Lần', 'SERVICE', FALSE, ARRAY['NAIL_SHOP'], 242, 'Chăm sóc bàn chân',   75)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ SPA_SHOP: fix product_type_code + update durations + add missing ─
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Massage thư giãn 60p';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 90  WHERE name = 'Massage thư giãn 90p';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 45  WHERE name = 'Massage đầu & cổ';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 45  WHERE name = 'Massage bàn chân';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 75  WHERE name = 'Massage đá nóng';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Massage tinh dầu';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Chăm sóc da mặt cơ bản';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 90  WHERE name = 'Chăm sóc da mặt chuyên sâu';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Nặn mụn';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 30  WHERE name = 'Đắp mặt nạ dưỡng ẩm';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 45  WHERE name = 'Tẩy tế bào chết toàn thân';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Ủ trắng toàn thân';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 15  WHERE name = 'Wax lông nách';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 45  WHERE name = 'Wax lông chân';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 30  WHERE name = 'Wax bikini';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Trị nám, tàn nhang';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 60  WHERE name = 'Trị mụn lưng';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 90  WHERE name = 'Combo mặt + massage';
UPDATE product_suggestions SET product_type_code = 'SERVICE', duration_minutes = 300 WHERE name = 'Liệu trình 5 buổi';

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Lột da hóa học (Peel) spa',         '✨', 350000, 'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 314, 'Chăm sóc da mặt',    45),
('Dưỡng ẩm tay chân (spa)',          '🧴', 200000, 'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 322, 'Chăm sóc cơ thể',    40),
('Quấn nóng giảm eo',                 '🧖', 500000, 'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 323, 'Chăm sóc cơ thể',    60),
('Wax lông chân (toàn chân)',         '✂️', 250000, 'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 333, 'Waxing & Triệt lông', 45),
('Wax mặt (môi, mày)',               '✂️', 60000,  'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 334, 'Waxing & Triệt lông', 15),
('Trị mụn lưng chuyên sâu (spa)',    '🫧', 350000, 'Lần', 'SERVICE', FALSE, ARRAY['SPA_SHOP'], 342, 'Điều trị đặc biệt',  60)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ PUB (display_order 1000+) ──────────────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Bia Saigon Special lon',            '🍺', 20000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1000, 'Bia lon & Bia chai', 0),
('Bia Tiger Crystal lon',             '🍺', 22000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1001, 'Bia lon & Bia chai', 0),
('Bia Heineken lon',                  '🍺', 25000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1002, 'Bia lon & Bia chai', 0),
('Bia 333 lon',                       '🍺', 18000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1003, 'Bia lon & Bia chai', 0),
('Bia Saigon Đỏ lon',                 '🍺', 15000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1004, 'Bia lon & Bia chai', 0),
('Két bia Saigon (24 lon)',            '🍺', 400000, 'Két',    'BEVERAGE', FALSE, ARRAY['PUB'], 1005, 'Két bia',            0),
('Két bia Tiger (24 lon)',             '🍺', 440000, 'Két',    'BEVERAGE', FALSE, ARRAY['PUB'], 1006, 'Két bia',            0),
('Rượu đế / Rượu gạo',               '🍶', 50000,  'Chai',   'BEVERAGE', FALSE, ARRAY['PUB'], 1007, 'Rượu mạnh',          0),
('Rượu Vodka Nếp Mới',               '🍶', 65000,  'Chai',   'BEVERAGE', FALSE, ARRAY['PUB'], 1008, 'Rượu mạnh',          0),
('Nước ngọt Coca-Cola (pub)',         '🥤', 12000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1009, 'Nước ngọt',          0),
('Nước ngọt Sprite',                  '🥤', 12000,  'Lon',    'BEVERAGE', FALSE, ARRAY['PUB'], 1010, 'Nước ngọt',          0),
('Nước suối chai (pub)',              '💧', 5000,   'Chai',   'BEVERAGE', FALSE, ARRAY['PUB'], 1011, 'Nước suối',          0),
('Đậu phộng rang muối',              '🥜', 30000,  'Đĩa',   'FOOD',     FALSE, ARRAY['PUB'], 1020, 'Mồi khô',            0),
('Khô mực nướng',                    '🦑', 80000,  'Đĩa',   'FOOD',     FALSE, ARRAY['PUB'], 1021, 'Mồi khô',            0),
('Hột vịt lộn',                       '🥚', 15000,  'Trứng',  'FOOD',     FALSE, ARRAY['PUB'], 1022, 'Trứng & Đặc sản',   0),
('Gà nướng muối ớt',                  '🍗', 180000, 'Con',    'FOOD',     FALSE, ARRAY['PUB'], 1023, 'Mồi tươi & nướng',  0),
('Xúc xích nướng',                    '🌭', 60000,  'Đĩa',   'FOOD',     FALSE, ARRAY['PUB'], 1024, 'Mồi tươi & nướng',  0),
('Nem nướng',                         '🥢', 70000,  'Đĩa',   'FOOD',     FALSE, ARRAY['PUB'], 1025, 'Mồi tươi & nướng',  0),
('Thịt heo nướng sả',                '🥩', 90000,  'Đĩa',   'FOOD',     FALSE, ARRAY['PUB'], 1026, 'Mồi tươi & nướng',  0),
('Canh chua cá (pub)',               '🍲', 75000,  'Tô',     'FOOD',     FALSE, ARRAY['PUB'], 1027, 'Canh & Lẩu',        0),
('Lẩu gà lá giang',                  '🍲', 180000, 'Nồi',   'FOOD',     FALSE, ARRAY['PUB'], 1028, 'Canh & Lẩu',        0),
('Cơm trắng (pub)',                  '🍚', 10000,  'Chén',   'FOOD',     FALSE, ARRAY['PUB'], 1029, 'Cơm & Bún',         0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ PUB_SEAFOOD (display_order 1100+) ─────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Tôm sú nướng muối ớt',             '🦐', 250000, 'Kg',    'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1100, 'Tôm & Cua',       0),
('Tôm hùm hấp bia',                  '🦞', 800000, 'Kg',    'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1101, 'Tôm & Cua',       0),
('Cua rang muối',                    '🦀', 350000, 'Con',   'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1102, 'Tôm & Cua',       0),
('Cua hấp bia',                      '🦀', 320000, 'Con',   'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1103, 'Tôm & Cua',       0),
('Ghẹ hấp sả gừng',                 '🦀', 280000, 'Con',   'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1104, 'Tôm & Cua',       0),
('Mực chiên giòn',                   '🦑', 180000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1110, 'Mực & Bạch tuộc', 0),
('Mực nướng sa tế',                  '🦑', 200000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1111, 'Mực & Bạch tuộc', 0),
('Bạch tuộc nướng',                  '🐙', 220000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1112, 'Mực & Bạch tuộc', 0),
('Nghêu hấp sả',                     '🐚', 120000, 'Kg',   'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1120, 'Nghêu & Ốc',      0),
('Sò điệp nướng mỡ hành',           '🐚', 200000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1121, 'Nghêu & Ốc',      0),
('Cá lóc nướng trui',                '🐟', 200000, 'Con',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1130, 'Cá tươi',         0),
('Cá hồi sashimi',                   '🐟', 350000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1131, 'Cá tươi',         0),
('Cá mú hấp xì dầu',                '🐟', 400000, 'Con',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1132, 'Cá tươi',         0),
('Lẩu hải sản thập cẩm',            '🍲', 350000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1140, 'Lẩu thập cẩm',    0),
('Lẩu tôm cua',                      '🍲', 420000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1141, 'Lẩu đặc sản',     0),
('Lẩu mắm hải sản',                 '🍲', 380000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_SEAFOOD'], 1142, 'Lẩu thập cẩm',    0),
('Bia Saigon Special (seafood)',     '🍺', 20000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB_SEAFOOD'], 1150, 'Bia lon & Bia chai', 0),
('Bia Heineken (seafood)',           '🍺', 25000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB_SEAFOOD'], 1151, 'Bia lon & Bia chai', 0),
('Rượu đế (seafood)',               '🍶', 50000,  'Chai', 'BEVERAGE', FALSE, ARRAY['PUB_SEAFOOD'], 1152, 'Rượu mạnh',         0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ PUB_GOAT (display_order 1200+) ────────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Thịt dê xào lăn',                  '🐐', 200000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1200, 'Dê xào & hấp',     0),
('Dê nướng bếp than (phần)',         '🔥', 250000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1201, 'Dê nướng',         0),
('Dê hấp gừng',                      '🐐', 220000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1202, 'Dê xào & hấp',     0),
('Dê sốt vang',                      '🍷', 200000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1203, 'Dê đặc sản',       0),
('Dồi dê nướng',                     '🐐', 150000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1204, 'Dê đặc sản',       0),
('Tiết canh dê',                     '🐐', 80000,  'Bát',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1205, 'Dê đặc sản',       0),
('Cháo dê hầm',                      '🍲', 100000, 'Tô',   'FOOD', FALSE, ARRAY['PUB_GOAT'], 1206, 'Dê đặc sản',       0),
('Dê tái chanh',                     '🐐', 180000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1207, 'Dê xào & hấp',     0),
('Lẩu dê',                           '🍲', 350000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1210, 'Lẩu dê thường',    0),
('Lẩu dê sốt vang',                  '🍷', 450000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1211, 'Lẩu dê đặc biệt',  0),
('Khô mực nướng (goat)',             '🦑', 80000,  'Đĩa',  'FOOD', FALSE, ARRAY['PUB_GOAT'], 1220, 'Mồi khô',          0),
('Bia Saigon Special (goat)',        '🍺', 20000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB_GOAT'], 1230, 'Bia lon & Bia chai', 0),
('Bia Heineken (goat)',              '🍺', 25000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB_GOAT'], 1231, 'Bia lon & Bia chai', 0),
('Rượu đế (goat)',                  '🍶', 50000,  'Chai', 'BEVERAGE', FALSE, ARRAY['PUB_GOAT'], 1232, 'Rượu mạnh',         0),
('Rượu Vodka (goat)',               '🍶', 60000,  'Chai', 'BEVERAGE', FALSE, ARRAY['PUB_GOAT'], 1233, 'Rượu mạnh',         0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ PUB_BEEF (display_order 1300+) ────────────────────────────
INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Bò nhúng dấm',                     '🥩', 280000, 'Phần', 'FOOD', FALSE, ARRAY['PUB_BEEF'], 1300, 'Bò xào & nhúng',  0),
('Bò nướng ngũ vị',                  '🔥', 250000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1301, 'Bò nướng',        0),
('Bò nướng lá lốt',                  '🥩', 180000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1302, 'Bò nướng',        0),
('Bò tái chanh',                     '🥩', 170000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1303, 'Bò đặc sản',      0),
('Bò xào rau cải',                   '🥩', 160000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1304, 'Bò xào & nhúng',  0),
('Bò xào sả ớt',                     '🥩', 170000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1305, 'Bò xào & nhúng',  0),
('Bắp bò kho gừng',                  '🥩', 180000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1306, 'Bò hầm & kho',    0),
('Gân bò hầm',                       '🥩', 150000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1307, 'Bò hầm & kho',    0),
('Bò kho bánh mì',                   '🍲', 120000, 'Tô',   'FOOD', FALSE, ARRAY['PUB_BEEF'], 1308, 'Bò hầm & kho',    0),
('Lẩu bò',                           '🍲', 320000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1310, 'Lẩu bò thường',   0),
('Lẩu bò nhúng dấm',                 '🍲', 380000, 'Nồi',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1311, 'Lẩu bò đặc biệt', 0),
('Khô bò gác bếp',                   '🥩', 120000, 'Đĩa',  'FOOD', FALSE, ARRAY['PUB_BEEF'], 1320, 'Mồi khô',         0),
('Bia Saigon Special (beef)',        '🍺', 20000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB_BEEF'], 1330, 'Bia lon & Bia chai', 0),
('Bia Heineken (beef)',              '🍺', 25000,  'Lon',  'BEVERAGE', FALSE, ARRAY['PUB_BEEF'], 1331, 'Bia lon & Bia chai', 0),
('Rượu đế (beef)',                  '🍶', 50000,  'Chai', 'BEVERAGE', FALSE, ARRAY['PUB_BEEF'], 1332, 'Rượu mạnh',         0),
('Rượu Whisky đá',                  '🥃', 80000,  'Ly',   'BEVERAGE', FALSE, ARRAY['PUB_BEEF'], 1333, 'Rượu mạnh',         0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ RESTAURANT: add RESTAURANT to existing matching entries + new entries ─
UPDATE product_suggestions SET
    shop_types = (SELECT array_agg(DISTINCT t ORDER BY t) FROM unnest(shop_types || ARRAY['RESTAURANT']) t)
WHERE name IN (
    'Phở bò', 'Bún bò Huế', 'Hủ tiếu Nam Vang', 'Cơm chiên dương châu',
    'Lẩu thái hải sản', 'Bánh mì thịt', 'Nước mắm'
) AND NOT ('RESTAURANT' = ANY(shop_types));

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Phở bò viên',                      '🍜', 55000,  'Tô',     'FOOD', FALSE, ARRAY['RESTAURANT'], 1400, 'Phở & Bún bò',    0),
('Cơm tấm sườn bì chả',              '🍚', 55000,  'Phần',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1401, 'Cơm phần',        0),
('Cơm gà xé phay',                   '🍚', 50000,  'Phần',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1402, 'Cơm phần',        0),
('Mì xào hải sản',                   '🍜', 65000,  'Phần',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1403, 'Mì & Hủ tiếu',    0),
('Gà xào sả ớt',                     '🍗', 75000,  'Đĩa',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1404, 'Món xào',         0),
('Bò xào rau muống',                 '🥩', 70000,  'Đĩa',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1405, 'Món xào',         0),
('Canh chua cá lóc',                 '🍲', 65000,  'Tô',     'FOOD', FALSE, ARRAY['RESTAURANT'], 1406, 'Canh & Súp',      0),
('Canh khổ qua hầm',                 '🍲', 45000,  'Tô',     'FOOD', FALSE, ARRAY['RESTAURANT'], 1407, 'Canh & Súp',      0),
('Gỏi bắp cải tôm thịt',            '🥗', 55000,  'Đĩa',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1408, 'Gỏi & Salad',     0),
('Nem cuốn tôm thịt',               '🫔', 45000,  'Đĩa',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1409, 'Khai vị',         0),
('Lẩu bò nhúng dấm (restaurant)',   '🍲', 200000, 'Nồi',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1410, 'Lẩu',             0),
('Cơm trắng (restaurant)',           '🍚', 10000,  'Chén',   'FOOD', FALSE, ARRAY['RESTAURANT'], 1411, 'Cơm phần',        0),
('Trà đá (restaurant)',              '🍵', 5000,   'Ly',     'BEVERAGE', FALSE, ARRAY['RESTAURANT'], 1420, 'Trà & Cà phê',  0),
('Bia Saigon (restaurant)',          '🍺', 20000,  'Lon',    'BEVERAGE', FALSE, ARRAY['RESTAURANT'], 1421, 'Bia & Rượu',    0),
('Nước cam tươi (restaurant)',       '🍊', 25000,  'Ly',     'BEVERAGE', FALSE, ARRAY['RESTAURANT'], 1422, 'Đồ uống',       0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ COFFEE_SHOP: add COFFEE_SHOP to existing matching entries + new entries ─
UPDATE product_suggestions SET
    shop_types = (SELECT array_agg(DISTINCT t ORDER BY t) FROM unnest(shop_types || ARRAY['COFFEE_SHOP']) t)
WHERE name IN (
    'Cà phê sữa đá', 'Bạc xỉu', 'Cà phê đen đá', 'Trà sữa trân châu',
    'Americano', 'Latte', 'Cappuccino', 'Nước ép cam', 'Sinh tố bơ',
    'Nước ép dứa', 'Bánh croissant', 'Sandwich', 'Bánh tiramisu'
) AND NOT ('COFFEE_SHOP' = ANY(shop_types));

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Cà phê đen nóng',                  '☕', 25000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1500, 'Cà phê đen',          0),
('Cà phê sữa nóng',                  '☕', 30000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1501, 'Cà phê sữa',          0),
('Bạc xỉu nóng',                     '☕', 30000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1502, 'Cà phê sữa',          0),
('Cà phê trứng',                     '☕', 40000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1503, 'Cà phê kem & đặc biệt',0),
('Cold brew',                         '☕', 45000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1504, 'Cà phê đen',          0),
('Cà phê dừa',                       '☕', 50000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1505, 'Cà phê kem & đặc biệt',0),
('Cà phê muối',                      '☕', 50000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1506, 'Cà phê kem & đặc biệt',0),
('Trà sữa trân châu trắng',          '🧋', 45000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1510, 'Trà sữa',             0),
('Trà sữa matcha',                   '🍵', 48000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1511, 'Trà sữa',             0),
('Trà đào cam sả đá',                '🍵', 45000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1512, 'Trà trái cây',        0),
('Trà vải',                          '🍵', 40000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1513, 'Trà trái cây',        0),
('Trà tắc mật ong',                  '🍵', 35000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1514, 'Trà thảo mộc',        0),
('Trà hoa cúc',                      '🍵', 35000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1515, 'Trà thảo mộc',        0),
('Sinh tố dâu',                      '🍓', 40000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1520, 'Sinh tố',             0),
('Sinh tố xoài',                     '🥭', 38000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1521, 'Sinh tố',             0),
('Nước ép táo',                      '🍏', 35000,  'Ly',    'BEVERAGE', FALSE, ARRAY['COFFEE_SHOP'], 1522, 'Nước ép trái cây',    0),
('Bánh croissant bơ',                '🥐', 35000,  'Cái',   'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 1530, 'Bánh ngọt',           0),
('Bánh cheesecake phô mai',          '🍰', 60000,  'Miếng', 'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 1531, 'Bánh ngọt',           0),
('Bánh brownie socola',              '🍫', 45000,  'Miếng', 'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 1532, 'Bánh ngọt',           0),
('Bánh mì sandwich trứng',           '🥪', 45000,  'Ổ',     'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 1533, 'Bánh mì & Sandwich',  0),
('Bánh mì bơ tỏi',                  '🥖', 25000,  'Ổ',     'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 1534, 'Bánh mì & Sandwich',  0),
('Khoai tây chiên',                  '🍟', 30000,  'Phần', 'FOOD',     FALSE, ARRAY['COFFEE_SHOP'], 1535, 'Bánh & Snack',        0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );

-- ─ CONVENIENCE_STORE: add/update entries (display_order 1600+) ─
UPDATE product_suggestions SET
    shop_types = (SELECT array_agg(DISTINCT t ORDER BY t) FROM unnest(shop_types || ARRAY['CONVENIENCE_STORE']) t),
    default_price = 12000
WHERE name = 'Coca Cola' AND NOT ('CONVENIENCE_STORE' = ANY(shop_types));

INSERT INTO product_suggestions
    (name, emoji, default_price, unit, product_type_code, dynamic_price, shop_types, display_order, category_name, duration_minutes)
VALUES
('Coca-Cola 330ml',                  '🥤', 12000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1600, 'Nước giải khát',           0),
('Pepsi 330ml',                      '🥤', 12000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1601, 'Nước giải khát',           0),
('7-Up 330ml',                       '🥤', 11000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1602, 'Nước giải khát',           0),
('Red Bull 250ml',                   '🔋', 13000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1603, 'Nước tăng lực',            0),
('Number One 330ml',                 '🔋', 10000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1604, 'Nước tăng lực',            0),
('Nước suối Aqua 500ml',             '💧', 6000,   'Chai',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1605, 'Nước suối / Nước tinh khiết',0),
('Nước suối La Vie 500ml',           '💧', 7000,   'Chai',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1606, 'Nước suối / Nước tinh khiết',0),
('Bia Tiger 330ml',                  '🍺', 18000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1607, 'Bia & Nước có cồn',        0),
('Bia Saigon Đỏ 330ml',             '🍺', 15000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1608, 'Bia & Nước có cồn',        0),
('Trà Olong Tea Plus 455ml',         '🍵', 12000,  'Chai',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1609, 'Trà & Cà phê đóng gói',   0),
('Trà xanh 0 Độ 455ml',             '🍵', 12000,  'Chai',   'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1610, 'Trà & Cà phê đóng gói',   0),
('Sting Dâu 330ml',                  '🔋', 10000,  'Lon',    'BEVERAGE', FALSE, ARRAY['CONVENIENCE_STORE'], 1611, 'Nước tăng lực',            0),
('Mì Hảo Hảo Tôm Chua Cay 75g',     '🍜', 7000,   'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1620, 'Mì gói & Cháo gói',        0),
('Mì 3 Miền Bò Hầm 65g',            '🍜', 6000,   'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1621, 'Mì gói & Cháo gói',        0),
('Phở Bò Ăn Liền Vifon 65g',        '🍜', 8000,   'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1622, 'Mì gói & Cháo gói',        0),
('Cháo Thịt Bằm Vifon 50g',         '🍲', 12000,  'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1623, 'Mì gói & Cháo gói',        0),
('Sữa TH True Milk 180ml',           '🥛', 8000,   'Hộp',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1624, 'Sữa & Sản phẩm sữa',       0),
('Sữa Vinamilk UHT 180ml',           '🥛', 7500,   'Hộp',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1625, 'Sữa & Sản phẩm sữa',       0),
('Nước mắm Nam Ngư 500ml',           '🫙', 18000,  'Chai',  'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1626, 'Gia vị & Nước chấm',       0),
('Dầu ăn Neptune 500ml',             '🫒', 42000,  'Chai',  'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1627, 'Gia vị & Nước chấm',       0),
('Mì chính Ajinomoto 100g',          '🧂', 12000,  'Gói',   'FOOD',     FALSE, ARRAY['CONVENIENCE_STORE'], 1628, 'Gia vị & Nước chấm',       0),
('Bánh Oreo 97g',                    '🍪', 18000,  'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1630, 'Bánh quy & Bánh ngọt',  0),
('Snack Poca Khoai Tây BBQ 68g',     '🍟', 12000,  'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1631, 'Snack khoai tây',        0),
('Kẹo Dừa Bến Tre 200g',            '🍬', 25000,  'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1632, 'Kẹo & Socola',           0),
('Bánh Kinh Đô Hương Vani',          '🍪', 22000,  'Hộp',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1633, 'Bánh quy & Bánh ngọt',  0),
('Kem đánh răng Colgate 150g',       '🪥', 35000,  'Tuýp',  'BEAUTY',  FALSE, ARRAY['CONVENIENCE_STORE'], 1640, 'Vệ sinh cá nhân',          0),
('Dầu gội Clear Mát Lạnh 170ml',    '🧴', 45000,  'Chai',  'BEAUTY',  FALSE, ARRAY['CONVENIENCE_STORE'], 1641, 'Vệ sinh cá nhân',          0),
('Xà phòng Lifebuoy 90g',           '🧼', 18000,  'Bánh', 'BEAUTY',  FALSE, ARRAY['CONVENIENCE_STORE'], 1642, 'Vệ sinh cá nhân',          0),
('Bàn chải Oral-B Classic',          '🪥', 25000,  'Cái',   'BEAUTY',  FALSE, ARRAY['CONVENIENCE_STORE'], 1643, 'Vệ sinh cá nhân',          0),
('Nước rửa chén Sunlight 500ml',    '🧹', 22000,  'Chai',  'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1650, 'Đồ gia dụng',            0),
('Bột giặt Omo Comfort 400g',       '🧺', 32000,  'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1651, 'Đồ gia dụng',            0),
('Thuốc lá Esse Menthol',           '🚬', 30000,  'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1660, 'Thuốc lá',               0),
('Thuốc lá 555 State Express',      '🚬', 35000,  'Gói',   'CONVENIENCE', FALSE, ARRAY['CONVENIENCE_STORE'], 1661, 'Thuốc lá',               0)
ON CONFLICT (name) DO UPDATE SET
    default_price    = EXCLUDED.default_price,
    duration_minutes = EXCLUDED.duration_minutes,
    product_type_code = EXCLUDED.product_type_code,
    category_name    = COALESCE(EXCLUDED.category_name, product_suggestions.category_name),
    shop_types       = (
        SELECT array_agg(DISTINCT t ORDER BY t)
        FROM unnest(product_suggestions.shop_types || EXCLUDED.shop_types) t
    );
