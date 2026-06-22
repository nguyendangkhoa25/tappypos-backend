-- ============================================================
-- V044 — Simplify the staff role model
-- ============================================================
-- The assignable role set is reduced to:
--     SHOP_OWNER, CASHIER, ACCOUNTANT, WAREHOUSE_STAFF, SERVICE_STAFF, TECHNICIAN
--
-- Removed roles and where their users go:
--     MANAGER       -> SHOP_OWNER   (near-owner access; promoted)
--     PAWN_OFFICER  -> CASHIER      (CASHIER inherited the pawn features)
--     RECEPTIONIST  -> CASHIER      (CASHIER inherited the front-desk / ROOM features)
--     CLEANER       -> CASHIER
--
-- Also: every tenant gains an ACCOUNTANT role.
--
-- RLS note: `roles` and `users` use ENABLE (not FORCE) ROW LEVEL SECURITY, so the
-- table owner — which Flyway connects as — bypasses the tenant_isolation policy and
-- can read/modify rows across all tenants. `user_roles` and `role_features` carry no
-- RLS policy at all. `employees` uses FORCE RLS and is intentionally NOT touched here:
-- EmployeePosition is an HR field decoupled from the access role and its enum is unchanged.
--
-- The whole migration is idempotent (NOT EXISTS / ON CONFLICT guards).

-- ── 1. Ensure every provisioned tenant has a CASHIER role ─────────────────────
-- Pawn / lodging tenants previously only had SHOP_OWNER plus a now-removed role, so
-- they may have no CASHIER row yet. A tenant is "provisioned" iff it has a SHOP_OWNER.
INSERT INTO roles (tenant_id, name, description)
SELECT DISTINCT so.tenant_id, 'CASHIER',
       'Cashier - Handles POS sales, customer transactions, front desk and pawn'
FROM   roles so
WHERE  so.name = 'SHOP_OWNER' AND so.deleted = FALSE AND so.tenant_id IS NOT NULL
  AND  NOT EXISTS (SELECT 1 FROM roles c
                   WHERE c.tenant_id = so.tenant_id AND c.name = 'CASHIER' AND c.deleted = FALSE);

-- ── 2. Ensure every provisioned tenant has an ACCOUNTANT role ─────────────────
INSERT INTO roles (tenant_id, name, description)
SELECT DISTINCT so.tenant_id, 'ACCOUNTANT',
       'Accountant - Manages revenue, salary, and invoices'
FROM   roles so
WHERE  so.name = 'SHOP_OWNER' AND so.deleted = FALSE AND so.tenant_id IS NOT NULL
  AND  NOT EXISTS (SELECT 1 FROM roles a
                   WHERE a.tenant_id = so.tenant_id AND a.name = 'ACCOUNTANT' AND a.deleted = FALSE);

-- ── 3. Grant CASHIER its (merged) feature set, scoped to features the tenant has ──
-- The tenant's available features = those held by its SHOP_OWNER role, so we never
-- grant a feature the shop itself wasn't given. Covers both freshly-created CASHIER
-- rows (step 1) and pre-existing ones (adds the newly-absorbed pawn/front-desk feats).
INSERT INTO role_features (role_id, feature_id)
SELECT c.id, f.id
FROM   roles c
JOIN   features f ON f.name IN (
           'DASHBOARD','MY_WORK','ORDER','POS','TABLE_SERVICE','CUSTOMER','LOYALTY','PROMOTION',
           'COMMISSION','NOTIFICATION','FEEDBACK','BOOKING','TRADE_IN','INSTALLMENT','CONSIGNMENT',
           'APPOINTMENT','ROOM','PRODUCT','PAWN','GOLD_PRICE','GOLD_PRICE_CHART','UTILITIES')
WHERE  c.name = 'CASHIER' AND c.deleted = FALSE AND c.tenant_id IS NOT NULL
  AND  EXISTS (SELECT 1 FROM roles so
               JOIN role_features sof ON sof.role_id = so.id
               WHERE so.tenant_id = c.tenant_id AND so.name = 'SHOP_OWNER' AND so.deleted = FALSE
                 AND sof.feature_id = f.id)
ON CONFLICT (role_id, feature_id) DO NOTHING;

-- ── 4. Grant ACCOUNTANT its feature set, scoped to features the tenant has ────────
INSERT INTO role_features (role_id, feature_id)
SELECT a.id, f.id
FROM   roles a
JOIN   features f ON f.name IN (
           'DASHBOARD','MY_WORK','REVENUE','EXPENSE','SALARY','INVOICE','ACCOUNTING','CUSTOMER',
           'CUSTOMER_DEBT','NOTIFICATION','FEEDBACK','INSTALLMENT','INSTALLMENT_VIEW_ALL','UTILITIES')
WHERE  a.name = 'ACCOUNTANT' AND a.deleted = FALSE AND a.tenant_id IS NOT NULL
  AND  EXISTS (SELECT 1 FROM roles so
               JOIN role_features sof ON sof.role_id = so.id
               WHERE so.tenant_id = a.tenant_id AND so.name = 'SHOP_OWNER' AND so.deleted = FALSE
                 AND sof.feature_id = f.id)
ON CONFLICT (role_id, feature_id) DO NOTHING;

-- ── 5. Reassign users off the removed roles (before deleting those roles) ─────────
-- 5a. MANAGER -> SHOP_OWNER
INSERT INTO user_roles (user_id, role_id)
SELECT ur.user_id, so.id
FROM   user_roles ur
JOIN   roles m  ON m.id = ur.role_id AND m.name = 'MANAGER'
JOIN   roles so ON so.tenant_id = m.tenant_id AND so.name = 'SHOP_OWNER' AND so.deleted = FALSE
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 5b. PAWN_OFFICER / RECEPTIONIST / CLEANER -> CASHIER (guaranteed to exist after step 1)
INSERT INTO user_roles (user_id, role_id)
SELECT ur.user_id, c.id
FROM   user_roles ur
JOIN   roles r ON r.id = ur.role_id AND r.name IN ('PAWN_OFFICER','RECEPTIONIST','CLEANER')
JOIN   roles c ON c.tenant_id = r.tenant_id AND c.name = 'CASHIER' AND c.deleted = FALSE
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ── 6. Drop the obsolete roles. FK cascades remove their user_roles + role_features. ─
DELETE FROM roles WHERE name IN ('MANAGER','PAWN_OFFICER','RECEPTIONIST','CLEANER');
