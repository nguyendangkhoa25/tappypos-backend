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

-- 2.2 gold_types — tuổi vàng / purity catalog
CREATE TABLE IF NOT EXISTS gold_types (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL,
    label      VARCHAR(250) NOT NULL,
    is_silver  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_gold_types_code UNIQUE (code)
);

-- 2.3 gold_brands — chành vàng / jewelry brand catalog
CREATE TABLE IF NOT EXISTS gold_brands (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL,
    label      VARCHAR(250) NOT NULL,
    name       VARCHAR(250) DEFAULT NULL,
    short_name VARCHAR(250) DEFAULT NULL,
    pub_stand  VARCHAR(250) DEFAULT NULL,
    address    VARCHAR(500) DEFAULT NULL,
    phone      VARCHAR(15)  DEFAULT NULL,
    origin     VARCHAR(50)  DEFAULT NULL,
    is_silver  BOOLEAN      NOT NULL DEFAULT FALSE,
    favourite  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_gold_brands_code UNIQUE (code)
);

-- ════════════════════════════════════════════════════════════
-- SECTION 3: Unified tables (tenant_id nullable)
--   NULL  = master record
--   value = shop record
-- ════════════════════════════════════════════════════════════

-- 3.1 features
CREATE TABLE IF NOT EXISTS features (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    VARCHAR(100) DEFAULT NULL,
    name         VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description  VARCHAR(500) DEFAULT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    DEFAULT NOW(),
    updated_at   TIMESTAMP    DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP    DEFAULT NULL
);
-- Unique name per scope (NULL scope = master, value scope = tenant)
CREATE UNIQUE INDEX IF NOT EXISTS uq_features_name_master ON features (name) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_features_name_tenant ON features (name, tenant_id) WHERE tenant_id IS NOT NULL;

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
    tenant_id VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- 3.5 role_features
CREATE TABLE IF NOT EXISTS role_features (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  VARCHAR(100) DEFAULT NULL,
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
    tenant_id   VARCHAR(100) DEFAULT NULL,
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

-- 3.9 activity_log
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
    CONSTRAINT fk_ag_product_type FOREIGN KEY (product_type_id) REFERENCES product_type (id)
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
    CONSTRAINT uq_product_sku_tenant     UNIQUE (sku, tenant_id),
    CONSTRAINT fk_product_type           FOREIGN KEY (product_type_id) REFERENCES product_type (id),
    CONSTRAINT fk_product_vendor         FOREIGN KEY (vendor_id)       REFERENCES vendors       (id)
);

-- 4.7 product_category
CREATE TABLE IF NOT EXISTS product_category (
    product_id  BIGINT       NOT NULL,
    category_id BIGINT       NOT NULL,
    tenant_id   VARCHAR(100) NOT NULL,
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
    created_at              TIMESTAMP      DEFAULT NOW(),
    updated_at              TIMESTAMP      DEFAULT NOW(),
    deleted                 BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_orders_number_tenant UNIQUE (order_number, tenant_id),
    CONSTRAINT chk_orders_status       CHECK  (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED','VOIDED')),
    CONSTRAINT fk_orders_customer      FOREIGN KEY (customer_id) REFERENCES customers (id)
);

-- 4.16 order_items
CREATE TABLE IF NOT EXISTS order_items (
    id                    BIGSERIAL      PRIMARY KEY,
    tenant_id             VARCHAR(100)   NOT NULL,
    order_id              BIGINT         NOT NULL,
    product_id            BIGINT         NOT NULL,
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
    assigned_employee_id  BIGINT         DEFAULT NULL,
    unit_cost             DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    cost_amount           DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    included_in_salary_id BIGINT         DEFAULT NULL,
    is_salary_calculated  BOOLEAN        NOT NULL DEFAULT FALSE,
    completed_at          TIMESTAMP      DEFAULT NULL,
    created_at            TIMESTAMP      DEFAULT NOW(),
    updated_at            TIMESTAMP      DEFAULT NOW(),
    deleted               BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP      DEFAULT NULL,
    CONSTRAINT chk_oi_status CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED')),
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

-- 4.17 invoices
CREATE TABLE IF NOT EXISTS invoices (
    id                       BIGSERIAL      PRIMARY KEY,
    tenant_id                VARCHAR(100)   NOT NULL,
    order_id                 BIGINT         NOT NULL,
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
    deleted                  BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMP      DEFAULT NULL,
    created_at               TIMESTAMP      DEFAULT NOW(),
    updated_at               TIMESTAMP      DEFAULT NOW(),
    CONSTRAINT uq_invoices_number_tenant UNIQUE (invoice_number, tenant_id),
    CONSTRAINT chk_inv_status CHECK (status IN ('DRAFT','COMPLETED','FAILED','CANCELLED')),
    CONSTRAINT fk_inv_order  FOREIGN KEY (order_id)  REFERENCES orders         (id),
    CONSTRAINT fk_inv_buyer  FOREIGN KEY (buyer_id)  REFERENCES invoice_buyers (id) ON DELETE SET NULL
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
    created_at               TIMESTAMP      DEFAULT NOW(),
    updated_at               TIMESTAMP      DEFAULT NOW(),
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
    CONSTRAINT uq_carts_id_tenant UNIQUE (cart_id, tenant_id)
);

-- 4.20 cart_items
CREATE TABLE IF NOT EXISTS cart_items (
    id              BIGSERIAL      PRIMARY KEY,
    tenant_id       VARCHAR(100)   NOT NULL,
    cart_id         BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    product_name    VARCHAR(255)   NOT NULL,
    sku             VARCHAR(100)   NOT NULL,
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
    variants        JSONB          DEFAULT NULL,
    notes           TEXT           DEFAULT NULL,
    added_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ci_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE
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
    user_id         BIGINT         DEFAULT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      DEFAULT NOW(),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP      DEFAULT NULL,
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

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
    interest_days_per_month  INT            DEFAULT NULL,
    pawned_days              INT            DEFAULT NULL,
    visible                  BOOLEAN        NOT NULL DEFAULT TRUE
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
    interest_days_per_month  INT            DEFAULT NULL,
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
    updated_at     TIMESTAMP      DEFAULT NULL
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

-- 4.35 shop_info
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
    created_by    VARCHAR(100)   DEFAULT NULL,
    updated_by    VARCHAR(100)   DEFAULT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP      DEFAULT NULL,
    CONSTRAINT uq_gold_price_code_tenant UNIQUE (code, tenant_id)
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
CREATE INDEX IF NOT EXISTS idx_features_tenant_id ON features (tenant_id);
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
CREATE INDEX IF NOT EXISTS idx_ur_tenant_id ON user_roles (tenant_id);

-- role_features
CREATE INDEX IF NOT EXISTS idx_rf_role_id    ON role_features (role_id);
CREATE INDEX IF NOT EXISTS idx_rf_feature_id ON role_features (feature_id);
CREATE INDEX IF NOT EXISTS idx_rf_tenant_id  ON role_features (tenant_id);

-- refresh_tokens
CREATE INDEX IF NOT EXISTS idx_rt_user_id   ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_rt_active    ON refresh_tokens (active);
CREATE INDEX IF NOT EXISTS idx_rt_tenant_id ON refresh_tokens (tenant_id);

-- active_sessions
CREATE INDEX IF NOT EXISTS idx_as_tenant_id ON active_sessions (tenant_id);

-- notifications
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read, deleted);
CREATE INDEX IF NOT EXISTS idx_notif_tenant_id ON notifications (tenant_id);

-- activity_log
CREATE INDEX IF NOT EXISTS idx_al_actor      ON activity_log (actor_username);
CREATE INDEX IF NOT EXISTS idx_al_action     ON activity_log (action);
CREATE INDEX IF NOT EXISTS idx_al_created_at ON activity_log (created_at);
CREATE INDEX IF NOT EXISTS idx_al_tenant_id  ON activity_log (tenant_id);

-- product_type
CREATE INDEX IF NOT EXISTS idx_pt_tenant_id  ON product_type (tenant_id);
CREATE INDEX IF NOT EXISTS idx_pt_deleted    ON product_type (deleted);

-- product
CREATE INDEX IF NOT EXISTS idx_product_tenant_id ON product (tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_status    ON product (status);
CREATE INDEX IF NOT EXISTS idx_product_deleted   ON product (deleted);
CREATE INDEX IF NOT EXISTS idx_product_created_at ON product (created_at);

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

-- order_items
CREATE INDEX IF NOT EXISTS idx_oi_tenant_id    ON order_items (tenant_id);
CREATE INDEX IF NOT EXISTS idx_oi_order_id     ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_oi_status       ON order_items (status);
CREATE INDEX IF NOT EXISTS idx_oi_employee     ON order_items (assigned_employee_id);

-- invoices
CREATE INDEX IF NOT EXISTS idx_inv_tenant_id   ON invoices (tenant_id);
CREATE INDEX IF NOT EXISTS idx_inv_order_id    ON invoices (order_id);
CREATE INDEX IF NOT EXISTS idx_inv_status      ON invoices (status);
CREATE INDEX IF NOT EXISTS idx_inv_deleted     ON invoices (deleted);

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

-- gold_types / gold_brands
CREATE INDEX IF NOT EXISTS idx_gt_active ON gold_types (active);
CREATE INDEX IF NOT EXISTS idx_gb_active ON gold_brands (active);

-- ════════════════════════════════════════════════════════════
-- SECTION 6: updated_at triggers
-- ════════════════════════════════════════════════════════════

DO $$
DECLARE t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'agents','banks','gold_types','gold_brands',
    'features','roles','users','role_features','notifications',
    'product_type','attribute_group','attribute_definition','category',
    'vendors','product','product_attribute_value','variant_types','variant_type_options',
    'inventory','inventory_movement','customers',
    'orders','order_items','invoices','invoice_items',
    'carts','cart_items','promotions',
    'loyalty_programs','loyalty_tiers','loyalty_transactions',
    'employees','purchase_orders','purchase_order_items',
    'market_prices','buyback_orders','buyback_order_items',
    'shop_info','shop_config','print_templates','bank_accounts','shop_expense',
    'gold_price','jewelry_counters','user_feedback'
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

ALTER TABLE features        ENABLE ROW LEVEL SECURITY;
ALTER TABLE features        FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON features
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE roles           ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles           FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON roles
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE users           FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE user_roles      ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles      FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON user_roles
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE role_features   ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_features   FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON role_features
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE refresh_tokens  ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens  FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON refresh_tokens
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE active_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE active_sessions FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON active_sessions
  USING (tenant_id IS NOT DISTINCT FROM current_tenant_id());

ALTER TABLE notifications   ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications   FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notifications
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
-- banks, gold_types, gold_brands are read-only reference data
-- accessible from any context.

-- ════════════════════════════════════════════════════════════
-- SECTION 8: Master seed data
-- All INSERT statements use ON CONFLICT DO NOTHING — safe to re-run.
-- tenant_id = NULL on all rows (master-scope records).
-- ════════════════════════════════════════════════════════════

-- ── 1. Platform features ──────────────────────────────────────
INSERT INTO features (id, tenant_id, name, display_name, description, active, deleted)
VALUES
    -- Shop management
    (202601001, NULL, 'DASHBOARD',        'Bảng Điều Khiển',          'Xem tổng quan và thống kê chính của cửa hàng',                   TRUE,  FALSE),
    (202601002, NULL, 'ORDER',            'Đơn Hàng',                  'Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',      TRUE,  FALSE),
    (202601003, NULL, 'MY_WORK',          'Công Việc Của Tôi',         'Xem công việc được giao cho nhân viên hiện tại',                  TRUE,  FALSE),
    (202601004, NULL, 'PRODUCT',          'Sản Phẩm & Dịch Vụ',       'Quản lý danh sách sản phẩm, dịch vụ, giá cả',                    TRUE,  FALSE),
    (202601005, NULL, 'PROMOTION',        'Khuyến Mãi',                'Tạo và quản lý các chương trình khuyến mãi, giảm giá',            TRUE,  FALSE),
    (202601006, NULL, 'EMPLOYEE',         'Nhân Viên',                 'Quản lý nhân viên, chức vụ, lương cơ bản',                       TRUE,  FALSE),
    (202601007, NULL, 'SALARY',           'Lương Nhân Viên',           'Quản lý bảng lương, tính toán lương, chi trả',                   TRUE,  FALSE),
    (202601008, NULL, 'CUSTOMER',         'Khách Hàng',                'Quản lý thông tin khách hàng, lịch sử mua hàng, tích điểm',      TRUE,  FALSE),
    (202601009, NULL, 'INVOICE',          'Hóa Đơn',                   'Quản lý hóa đơn, xuất hóa đơn điện tử',                         TRUE,  FALSE),
    (202601010, NULL, 'REVENUE',          'Doanh Thu',                 'Xem báo cáo doanh thu, lợi nhuận, chi phí',                      TRUE,  FALSE),
    (202601011, NULL, 'USER',             'Người Dùng',                'Quản lý tài khoản người dùng, quyền truy cập',                   TRUE,  FALSE),
    (202601012, NULL, 'SHOP_INFO',        'Thông Tin Cửa Hàng',        'Cập nhật thông tin cửa hàng, cấu hình hệ thống',                 TRUE,  FALSE),
    -- Master / system management
    (202601013, NULL, 'TENANT_MGMT',      'Quản Lý Cửa Hàng',         'Tạo, kích hoạt và quản lý các cửa hàng trong hệ thống',          TRUE,  FALSE),
    -- Supply chain
    (202601014, NULL, 'VENDOR',           'Nhà Cung Cấp',              'Quản lý nhà cung cấp và đơn đặt hàng nhập',                      TRUE,  FALSE),
    (202601015, NULL, 'AGENT_MGMT',       'Đại Lý',                    'Super admin quản lý đại lý và giao shop',                        TRUE,  FALSE),
    -- Operations
    (202601016, NULL, 'INVENTORY',        'Quản Lý Kho',               'Quản lý tồn kho, nhập xuất kho và kiểm kho',                     TRUE,  FALSE),
    (202601017, NULL, 'POS',              'Điểm Bán Hàng',             'Bán hàng tại quầy, thanh toán và in hóa đơn',                    TRUE,  FALSE),
    (202601018, NULL, 'ACTIVITY_LOG',     'Nhật Ký Hoạt Động',         'Xem nhật ký hoạt động của người dùng trong cửa hàng',            TRUE,  FALSE),
    (202601019, NULL, 'PAWN',             'Cầm Đồ',                    'Quản lý hợp đồng cầm đồ, lãi suất và thanh lý tài sản',          TRUE,  FALSE),
    (202601020, NULL, 'FEEDBACK_MGMT',    'Quản Lý Phản Hồi',          'Xem và xử lý phản hồi, góp ý từ người dùng toàn hệ thống',       TRUE,  FALSE),
    (202601021, NULL, 'MASTER_DASHBOARD', 'Bảng Điều Khiển Hệ Thống', 'Xem tổng quan và thống kê của hệ thống master',                  TRUE,  FALSE),
    (202601022, NULL, 'LOYALTY',          'Tích Điểm Khách Hàng',      'Chương trình tích điểm và phần thưởng khách hàng',               TRUE,  FALSE),
    (202601023, NULL, 'EXPENSE',          'Chi Phí',                   'Theo dõi và quản lý chi phí hoạt động cửa hàng',                 TRUE,  FALSE),
    (202601024, NULL, 'NOTIFICATION',     'Thông Báo',                 'Nhận thông báo và nhắc nhở từ hệ thống',                         TRUE,  FALSE),
    (202601025, NULL, 'FEEDBACK',         'Góp Ý',                     'Gửi phản hồi và đề xuất đến quản trị hệ thống',                  TRUE,  FALSE),
    (202601026, NULL, 'PRINT_TEMPLATE',   'Mẫu In',                    'Quản lý mẫu in biên nhận và hóa đơn',                            TRUE,  FALSE),
    (202601027, NULL, 'BANK_ACCOUNT',     'Tài Khoản Ngân Hàng',       'Quản lý tài khoản ngân hàng của cửa hàng',                       TRUE,  FALSE),
    (202601028, NULL, 'ACCOUNTING',       'Kế Toán',                   'Xem báo cáo kế toán tổng hợp',                                   TRUE,  FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('features', 'id'), 202601028, true);

-- ── 2. Master roles ───────────────────────────────────────────
INSERT INTO roles (id, tenant_id, name, description, deleted)
VALUES
    (202600001, NULL, 'MASTER_TENANT', 'Quản trị hệ thống - Toàn quyền quản lý tenant và người dùng master', FALSE),
    (202600002, NULL, 'AGENT',              'Quản trị đại lý - Quản lý danh sách shop thuộc đại lý', FALSE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('roles', 'id'), 202600002, true);

-- ── 3. Role-feature mappings (master roles) ───────────────────
INSERT INTO role_features (tenant_id, role_id, feature_id)
VALUES
    -- MASTER_TENANT: user, tenant, vendor, activity, feedback, master dashboard
    (NULL, 202600001, 202601011),   -- USER
    (NULL, 202600001, 202601013),   -- TENANT_MGMT
    (NULL, 202600001, 202601015),   -- AGENT_MGMT
    (NULL, 202600001, 202601018),   -- ACTIVITY_LOG
    (NULL, 202600001, 202601020),   -- FEEDBACK_MGMT
    (NULL, 202600001, 202601021),   -- MASTER_DASHBOARD
    -- AGENT: tenant management + master dashboard
    (NULL, 202600002, 202601013),   -- TENANT_MGMT
    (NULL, 202600002, 202601021)    -- MASTER_DASHBOARD
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

INSERT INTO user_roles (user_id, role_id, tenant_id)
VALUES (79260001, 202600001, NULL)
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
