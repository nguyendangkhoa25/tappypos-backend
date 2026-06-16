-- ============================================================
-- V008 — Guest room requests (ROOM feature: QR ordering + reception inbox)
-- A guest scans the in-room QR and either orders minibar items (added to the
-- stay folio with source=QR) or sends a request to reception. This table backs
-- the reception inbox; QR folio orders reuse room_stay_item.
-- ============================================================

CREATE TABLE IF NOT EXISTS room_request (
    id            BIGSERIAL     PRIMARY KEY,
    tenant_id     VARCHAR(100)  NOT NULL,
    room_id       BIGINT        NOT NULL,
    room_number   VARCHAR(50)   NOT NULL,
    stay_id       BIGINT        DEFAULT NULL,
    request_type  VARCHAR(30)   NOT NULL,                       -- SERVICE | CLEANING | SUPPLIES | CHECKOUT | OTHER
    message       TEXT          DEFAULT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'NEW',         -- NEW | IN_PROGRESS | DONE | CANCELLED
    handled_by    VARCHAR(255)  DEFAULT NULL,
    handled_at    TIMESTAMP     DEFAULT NULL,
    legacy_id     VARCHAR(50)   DEFAULT NULL,
    created_at    TIMESTAMP     DEFAULT NOW(),
    updated_at    TIMESTAMP     DEFAULT NOW(),
    deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP     DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_room_request_status ON room_request (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_room_request_room ON room_request (room_id);
CREATE INDEX IF NOT EXISTS idx_room_request_legacy_id ON room_request (tenant_id, legacy_id) WHERE legacy_id IS NOT NULL;

ALTER TABLE room_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE room_request FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON room_request
    USING (tenant_id = current_setting('app.current_tenant', true));
