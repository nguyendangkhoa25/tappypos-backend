-- Unify the subscription catalog (see docs/SUBSCRIPTION_PRICING_PLAN.md).
--
-- The catalog is now: TRIAL, BASIC (Cơ bản), PRO (Chuyên nghiệp),
-- ENTERPRISE (Doanh nghiệp), GOLD_PAWN (Vàng & Cầm đồ).
--
-- The legacy codes STARTER and PREMIUM no longer exist. Remap existing tenants so
-- SubscriptionPlan.of() (which now fails loudly on unknown codes) keeps working:
--   STARTER (1 user) -> BASIC (Cơ bản, 2 users)
--   PREMIUM (5 users, all features) -> PRO (Chuyên nghiệp, 6 users)
--
-- `tenants` is a master table (no RLS), so plain UPDATEs are correct here.

UPDATE tenants SET subscription_type = 'BASIC'
 WHERE UPPER(subscription_type) = 'STARTER';

UPDATE tenants SET subscription_type = 'PRO'
 WHERE UPPER(subscription_type) = 'PREMIUM';

-- Normalize any casing drift to upper-case so the backend lookup is exact.
UPDATE tenants
   SET subscription_type = UPPER(subscription_type)
 WHERE subscription_type IS NOT NULL
   AND subscription_type <> UPPER(subscription_type);
