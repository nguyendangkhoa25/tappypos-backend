-- V004 — Subscription payments (Phase 3b).
--
-- Platform-level (master) table: billing is not tenant-isolated business data, so it carries NO RLS
-- policy. Queries filter by tenant_id explicitly. The amount is derived server-side from
-- SubscriptionPlan.LIMITS — never trusted from the client. provider_txn_ref is our idempotency key
-- (a provider callback may arrive more than once).

CREATE TABLE IF NOT EXISTS subscription_payment (
    id                BIGSERIAL    PRIMARY KEY,
    tenant_id         VARCHAR(100) NOT NULL,
    provider          VARCHAR(20)  NOT NULL,   -- MOMO | VNPAY | VIETQR | APPLE_IAP | GOOGLE_IAP
    plan_code         VARCHAR(20)  NOT NULL,   -- STARTER | BASIC | PRO | ENTERPRISE | GOLD_PAWN
    billing_cycle     VARCHAR(10)  NOT NULL,   -- MONTHLY | YEARLY
    amount            BIGINT       NOT NULL,   -- VND, no decimals
    currency          VARCHAR(8)   NOT NULL DEFAULT 'VND',
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING | PAID | FAILED | REFUNDED
    provider_txn_ref  VARCHAR(100) NOT NULL,
    description       VARCHAR(255),
    raw_payload       TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    paid_at           TIMESTAMP,
    CONSTRAINT uq_subscription_payment_txn_ref UNIQUE (provider_txn_ref)
);

CREATE INDEX IF NOT EXISTS idx_subscription_payment_tenant
    ON subscription_payment (tenant_id, created_at DESC);
