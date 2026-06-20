-- ══════════════════════════════════════════════════════════════════════════════
-- Peak/off-peak rate tiers + minimum charge — SPORT_COURT / BILLIARDS_HALL.
-- Evening courts cost more (giá giờ vàng) and a session is floored at a minimum.
--   • booking_resource_rate: per-resource rate windows. day_kind ALL|WEEKDAY|WEEKEND,
--     a start–end time-of-day window → rate. checkout() picks the matching window by
--     the play time, falling back to booking_resources.hourly_rate when none matches.
--   • booking_resources.minimum_charge: the session total is floored at this value.
-- New tenant table → RLS + legacy_id per convention.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE booking_resources ADD COLUMN IF NOT EXISTS minimum_charge DECIMAL(15,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS booking_resource_rate (
    id            BIGSERIAL     PRIMARY KEY,
    tenant_id     VARCHAR(50)   NOT NULL,
    resource_id   BIGINT        NOT NULL REFERENCES booking_resources(id),
    day_kind      VARCHAR(10)   NOT NULL DEFAULT 'ALL',   -- ALL | WEEKDAY | WEEKEND
    start_time    TIME          NOT NULL,
    end_time      TIME          NOT NULL,
    rate          DECIMAL(15,2) NOT NULL DEFAULT 0,
    sort_order    INT           NOT NULL DEFAULT 0,
    legacy_id     VARCHAR(50)   DEFAULT NULL,
    created_by    VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP
);

-- ----- RLS -----
ALTER TABLE booking_resource_rate ENABLE ROW LEVEL SECURITY;
ALTER TABLE booking_resource_rate FORCE  ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies
        WHERE tablename = 'booking_resource_rate' AND policyname = 'booking_resource_rate_tenant_isolation') THEN
        CREATE POLICY booking_resource_rate_tenant_isolation ON booking_resource_rate
            USING (tenant_id = current_setting('app.current_tenant', true));
    END IF;
END $$;

-- ----- Indexes -----
CREATE INDEX IF NOT EXISTS idx_booking_resource_rate_resource
    ON booking_resource_rate (tenant_id, resource_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_booking_resource_rate_legacy
    ON booking_resource_rate (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;
