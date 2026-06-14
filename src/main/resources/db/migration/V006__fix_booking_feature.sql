-- Fix: the BOOKING feature was never actually inserted.
-- V004__bookings.sql used a hard-coded id (202601039) that already belongs to
-- TABLE_SERVICE in the canonical V001 schema, so its
-- `INSERT ... ON CONFLICT (id) DO NOTHING` silently no-op'd and the BOOKING
-- feature row is absent on a fresh database. V004 cannot be edited in place
-- (it is already applied in deployed environments — Flyway would fail on a
-- checksum mismatch), so this forward migration inserts it by name with a
-- sequence-assigned id. Idempotent: a no-op where BOOKING already exists.
INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('BOOKING', 'Đặt Bàn / Đặt Sân',
        'Quản lý bàn bida, sân thể thao: tính giờ chơi, đặt sân theo giờ và tạo hoá đơn khi kết thúc',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- Backfill role_features for tenants that were already flagged for BOOKING
-- (it is in their tenants.features CSV) but never got the role mapping because
-- the feature row didn't exist. Grant to SHOP_OWNER so the owner can use it;
-- staff roles can be re-assigned via role management. role_features has no RLS.
INSERT INTO role_features (role_id, feature_id)
SELECT r.id, f.id
FROM roles r
JOIN tenants t ON t.tenant_id = r.tenant_id
CROSS JOIN features f
WHERE f.name = 'BOOKING'
  AND r.name = 'SHOP_OWNER'
  AND r.tenant_id IS NOT NULL
  AND r.deleted = FALSE
  AND 'BOOKING' = ANY (string_to_array(COALESCE(t.features, ''), ','))
ON CONFLICT DO NOTHING;
