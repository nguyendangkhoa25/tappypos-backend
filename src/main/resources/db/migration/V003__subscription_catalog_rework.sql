-- V003 — Subscription catalog rework (2026-06)
--
-- Business change:
--   * TRIAL plan removed. Every new shop now starts on BASIC, free for 6 months from
--     registration (SubscriptionPlan.FREE_TRIAL_MONTHS), then goes read-only.
--   * STARTER re-introduced as a distinct 1-user tier (was previously remapped to BASIC).
--   * PRO max users 6 -> 4, ENTERPRISE 15 -> 10 (limits live in SubscriptionPlan.java, not the DB;
--     this migration only normalises the per-tenant subscription_type / max_users columns).
--
-- Existing TRIAL tenants keep their already-granted expiration_date (they were promised a free
-- year) — we only relabel them onto BASIC so SubscriptionPlan.of() no longer fails on the code.

-- 1. Relabel any tenant still on the removed TRIAL plan onto the new default (BASIC).
UPDATE tenants
SET    subscription_type = 'BASIC'
WHERE  upper(coalesce(subscription_type, '')) = 'TRIAL';

-- 2. Backfill a default plan for tenants that never had one set, so the read path is deterministic.
UPDATE tenants
SET    subscription_type = 'BASIC'
WHERE  subscription_type IS NULL OR btrim(subscription_type) = '';

-- 3. Clamp max_users down to the new ceilings for tenants whose stored value exceeds the
--    reduced PRO (4) / ENTERPRISE (10) limits. Tenants below the ceiling are left untouched.
UPDATE tenants SET max_users = 4  WHERE upper(subscription_type) = 'PRO'        AND max_users > 4;
UPDATE tenants SET max_users = 10 WHERE upper(subscription_type) = 'ENTERPRISE' AND max_users > 10;
