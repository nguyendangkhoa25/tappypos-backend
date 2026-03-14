-- V022: Promotions table + promotion/loyalty discount fields on orders

CREATE TABLE IF NOT EXISTS promotions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(200)    NOT NULL,
    code                VARCHAR(50)     NOT NULL UNIQUE,
    type                VARCHAR(20)     NOT NULL COMMENT 'AMOUNT or PERCENTAGE',
    value               DECIMAL(10,2)   NOT NULL,
    min_order_amount    DECIMAL(10,2)   NULL,
    max_discount_amount DECIMAL(10,2)   NULL,
    start_date          DATETIME        NULL,
    end_date            DATETIME        NULL,
    usage_limit         INT             NULL,
    used_count          INT             NOT NULL DEFAULT 0,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    description         VARCHAR(500)    NULL,
    deleted             TINYINT(1)      NOT NULL DEFAULT 0,
    deleted_at          DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS promotion_code        VARCHAR(50)    NULL    AFTER voided_by,
    ADD COLUMN IF NOT EXISTS promotion_discount    DECIMAL(10,2)  NOT NULL DEFAULT 0.00 AFTER promotion_code,
    ADD COLUMN IF NOT EXISTS loyalty_points_redeemed INT          NOT NULL DEFAULT 0    AFTER promotion_discount,
    ADD COLUMN IF NOT EXISTS loyalty_discount      DECIMAL(10,2)  NOT NULL DEFAULT 0.00 AFTER loyalty_points_redeemed;
