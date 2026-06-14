-- ── UTILITIES feature ────────────────────────────────────────────────────────
-- A client-side hub of calculators/tools (interest, loan, tax, budget, currency,
-- market gold prices, bill splitter, breakeven). No backend endpoint is gated by
-- this feature — it only controls the web route + sidebar visibility.
-- Scoped to jewelry / pawn / F&B shop types (matching FEATURE_PROFILES in
-- TenantProvisioningService); new shops of those types receive it at provisioning.
-- Insert by name with a sequence-assigned id (do NOT hardcode an id — the
-- 2026010xx range is already taken by V001 features and collides).
INSERT INTO features (name, display_name, description, active, deleted)
VALUES ('UTILITIES', 'Tiện Ích',
        'Bộ công cụ tính toán: tính lãi, khoản vay, thuế, ngân sách, đổi tiền, giá vàng thị trường, chia hóa đơn, điểm hòa vốn',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- Shop types that expose UTILITIES (pawn, jewelry, and all F&B variants).
-- Backfill the tenant-level feature CSV for existing shops of those types.
UPDATE tenants
SET features = CASE
        WHEN features IS NULL OR features = '' THEN 'UTILITIES'
        ELSE features || ',UTILITIES'
    END
WHERE shop_type IN ('PAWN_SHOP', 'JEWELRY', 'FOOD_BEVERAGE', 'COFFEE_SHOP',
                    'RESTAURANT', 'PUB', 'PUB_SEAFOOD', 'PUB_GOAT', 'PUB_BEEF',
                    'BILLIARDS_HALL')
  AND 'UTILITIES' <> ALL (string_to_array(COALESCE(features, ''), ','));

-- Backfill role_features for every (non-master) role belonging to those tenants
-- so existing staff see the hub. role_features has no RLS (FK-scoped).
INSERT INTO role_features (role_id, feature_id)
SELECT r.id, f.id
FROM roles r
JOIN tenants t ON t.tenant_id = r.tenant_id
CROSS JOIN features f
WHERE f.name = 'UTILITIES'
  AND r.tenant_id IS NOT NULL
  AND r.deleted = FALSE
  AND t.shop_type IN ('PAWN_SHOP', 'JEWELRY', 'FOOD_BEVERAGE', 'COFFEE_SHOP',
                      'RESTAURANT', 'PUB', 'PUB_SEAFOOD', 'PUB_GOAT', 'PUB_BEEF',
                      'BILLIARDS_HALL')
ON CONFLICT DO NOTHING;
