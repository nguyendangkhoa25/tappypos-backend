-- V023: Vendors and Purchase Orders

CREATE TABLE IF NOT EXISTS vendors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    code            VARCHAR(50)     NOT NULL UNIQUE,
    contact_name    VARCHAR(100)    NULL,
    email           VARCHAR(100)    NULL,
    phone           VARCHAR(20)     NULL,
    address         VARCHAR(300)    NULL,
    tax_id          VARCHAR(50)     NULL,
    payment_terms   VARCHAR(20)     NOT NULL DEFAULT 'NET_30',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    notes           VARCHAR(500)    NULL,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    deleted_at      DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number       VARCHAR(30)     NOT NULL UNIQUE,
    vendor_id       BIGINT          NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    total_amount    DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    expected_date   DATE            NULL,
    ordered_at      DATETIME        NULL,
    received_at     DATETIME        NULL,
    created_by      VARCHAR(100)    NULL,
    notes           VARCHAR(500)    NULL,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    deleted_at      DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_po_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id   BIGINT          NOT NULL,
    product_id          BIGINT          NULL,
    product_name        VARCHAR(255)    NOT NULL,
    product_sku         VARCHAR(100)    NULL,
    quantity_ordered    INT             NOT NULL,
    quantity_received   INT             NOT NULL DEFAULT 0,
    unit_cost           DECIMAL(15,2)   NOT NULL,
    total_cost          DECIMAL(15,2)   NOT NULL,
    deleted             TINYINT(1)      NOT NULL DEFAULT 0,
    deleted_at          DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_poi_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id)
);

-- Add VENDOR feature to master features table (safe to run multiple times)
INSERT INTO features (id, name, display_name, description, active, created_at, updated_at, deleted, deleted_at)
VALUES(202602004, 'VENDOR', 'Nhà Cung Cấp', 'Quản lý nhà cung cấp, đơn đặt hàng, nhập hàng', 1, '2026-03-17 00:00:00', '2026-03-17 00:00:00', 0, NULL);

