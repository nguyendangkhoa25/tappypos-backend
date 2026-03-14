-- ─────────────────────────────────────────────────────────────────────────────
-- V021: Loyalty Program System
-- Adds loyalty_programs, loyalty_tiers, loyalty_transactions tables
-- and loyalty fields to the customers table.
-- ─────────────────────────────────────────────────────────────────────────────

-- Add loyalty fields to existing customers table
ALTER TABLE customers
    ADD COLUMN loyalty_points INT NOT NULL DEFAULT 0 AFTER permanent_address,
    ADD COLUMN total_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00 AFTER loyalty_points;

-- Loyalty program settings (one row per tenant)
CREATE TABLE loyalty_programs (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    points_per_amount                INT            NOT NULL DEFAULT 1       COMMENT 'Points earned per unit of amount_per_points',
    amount_per_points                BIGINT         NOT NULL DEFAULT 10000   COMMENT 'VND required to earn points_per_amount points',
    redemption_points_per_discount   INT            NOT NULL DEFAULT 100     COMMENT 'Points needed to get redemption_discount_amount VND off',
    redemption_discount_amount       DECIMAL(10,2)  NOT NULL DEFAULT 10000   COMMENT 'VND discount per redemption unit',
    min_redemption_points            INT            NOT NULL DEFAULT 100     COMMENT 'Minimum points balance required to redeem',
    is_active                        TINYINT(1)     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Loyalty membership tiers
CREATE TABLE loyalty_tiers (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100)  NOT NULL,
    min_spend         DECIMAL(15,2) NOT NULL DEFAULT 0.00  COMMENT 'Minimum lifetime spend (VND) to reach this tier',
    points_multiplier DECIMAL(5,2)  NOT NULL DEFAULT 1.00  COMMENT 'Point earning multiplier at this tier',
    color             VARCHAR(20)   DEFAULT '#9E9E9E'       COMMENT 'Hex color for UI display',
    description       VARCHAR(500),
    sort_order        INT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at        TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_min_spend (min_spend)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Loyalty point transactions
CREATE TABLE loyalty_transactions (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id    BIGINT        NOT NULL,
    order_id       BIGINT        DEFAULT NULL,
    type           ENUM('EARNED','REDEEMED','ADJUSTED','EXPIRED') NOT NULL,
    points         INT           NOT NULL COMMENT 'Positive = earned/added; negative = redeemed/expired',
    balance_before INT           NOT NULL DEFAULT 0,
    balance_after  INT           NOT NULL DEFAULT 0,
    description    VARCHAR(500),
    created_at     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at     TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_customer_id (customer_id),
    KEY idx_order_id (order_id),
    CONSTRAINT fk_lt_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_lt_order    FOREIGN KEY (order_id)    REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default loyalty program
INSERT INTO loyalty_programs
    (points_per_amount, amount_per_points, redemption_points_per_discount, redemption_discount_amount, min_redemption_points, is_active)
VALUES (1, 10000, 100, 10000.00, 100, 1);

-- Default tiers
INSERT INTO loyalty_tiers (name, min_spend, points_multiplier, color, description, sort_order) VALUES
    ('Đồng',     0,          1.00, '#CD7F32', 'Thành viên cơ bản',        1),
    ('Bạc',      2000000,    1.25, '#9E9E9E', 'Chi tiêu từ 2 triệu VND',  2),
    ('Vàng',     10000000,   1.50, '#FFC107', 'Chi tiêu từ 10 triệu VND', 3),
    ('Kim cương', 50000000,  2.00, '#00BCD4', 'Chi tiêu từ 50 triệu VND', 4);
