-- Invoice tables for retail-platform tenant databases

CREATE TABLE IF NOT EXISTS invoices (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_number          VARCHAR(30)     NOT NULL UNIQUE,
    invoice_series          VARCHAR(20)     NULL,
    issued_date             DATETIME        NULL,
    total_amount_without_tax DECIMAL(15,2)  NULL,
    total_amount            DECIMAL(15,2)   NOT NULL,
    tax_amount              DECIMAL(15,2)   DEFAULT 0,
    tax_percentage          DECIMAL(5,2)    DEFAULT 0,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    payment_type            VARCHAR(50)     NULL,
    invoice_type            VARCHAR(50)     NULL,
    currency_code           VARCHAR(10)     DEFAULT 'VND',
    external_invoice_id     VARCHAR(100)    NULL,
    external_sync_at        DATETIME        NULL,
    error_message           VARCHAR(1000)   NULL,
    notes                   VARCHAR(500)    NULL,
    created_by              VARCHAR(100)    NULL,
    -- Embedded buyer info
    buyer_name              VARCHAR(200)    NULL,
    buyer_legal_name        VARCHAR(200)    NULL,
    buyer_tax_code          VARCHAR(50)     NULL,
    buyer_address_line      VARCHAR(500)    NULL,
    buyer_phone_number      VARCHAR(20)     NULL,
    buyer_email             VARCHAR(200)    NULL,
    buyer_bank_name         VARCHAR(200)    NULL,
    buyer_bank_account      VARCHAR(50)     NULL,
    buyer_id_number         VARCHAR(50)     NULL,
    visiting_guest          BOOLEAN         DEFAULT FALSE,
    customer_id             BIGINT          NULL,
    -- Base entity fields
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     NULL ON UPDATE CURRENT_TIMESTAMP(6),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at              DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_invoice_number (invoice_number),
    INDEX idx_invoices_deleted (deleted),
    INDEX idx_invoices_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_items (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_id                  BIGINT          NOT NULL,
    order_item_id               BIGINT          NULL,
    order_id                    BIGINT          NULL,
    line_number                 INT             NULL,
    service_name                VARCHAR(200)    NULL,
    service_code                VARCHAR(50)     NULL,
    unit                        VARCHAR(50)     NULL,
    unit_price                  DECIMAL(15,2)   NULL,
    quantity                    DECIMAL(10,3)   NULL,
    discount                    DECIMAL(10,2)   DEFAULT 0,
    total_amount_without_tax    DECIMAL(15,2)   NULL,
    tax_percentage              DECIMAL(5,2)    DEFAULT 0,
    tax_amount                  DECIMAL(15,2)   DEFAULT 0,
    total_amount_with_tax       DECIMAL(15,2)   NULL,
    -- Base entity fields
    created_at                  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)     NULL ON UPDATE CURRENT_TIMESTAMP(6),
    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                  DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_invoice_items_invoice_id (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add FK from orders to invoices (invoice_id column already exists)
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL;
