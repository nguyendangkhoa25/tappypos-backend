-- V004__shop_type_role_whitelist.sql
-- Enforce 2-role model (SHOP_OWNER + PAWN_OFFICER) for existing PAWN_SHOP and JEWELRY tenants.
-- New tenants are handled at provisioning time by TenantProvisioningService.applyShopTypeFilters().
--
-- Safe ordering:
--   1. Strip surplus roles from role_features (FK child) first.
--   2. Strip surplus roles from user_roles (FK child) next.
--   3. Soft-delete the surplus role rows last.
--
-- Surplus = any role other than SHOP_OWNER or PAWN_OFFICER for these shop types.

DO $$
DECLARE
    surplus_roles TEXT[] := ARRAY[
        'MANAGER', 'CASHIER', 'ACCOUNTANT', 'WAREHOUSE_STAFF',
        'SERVICE_STAFF', 'TECHNICIAN', 'RECEPTIONIST', 'CLEANER'
    ];
    affected_tenants TEXT[];
BEGIN
    -- Collect affected tenant IDs
    SELECT ARRAY_AGG(tenant_id)
    INTO affected_tenants
    FROM tenants
    WHERE shop_type IN ('PAWN_SHOP', 'JEWELRY')
      AND active = TRUE;

    IF affected_tenants IS NULL OR array_length(affected_tenants, 1) = 0 THEN
        RAISE NOTICE 'No active PAWN_SHOP or JEWELRY tenants found — nothing to clean up.';
        RETURN;
    END IF;

    RAISE NOTICE 'Cleaning up surplus roles for % tenant(s): %',
        array_length(affected_tenants, 1), affected_tenants;

    -- 1. Remove role_features rows for surplus roles in these tenants
    DELETE FROM role_features rf
    USING roles r
    WHERE rf.role_id = r.id
      AND r.tenant_id = ANY(affected_tenants)
      AND r.name = ANY(surplus_roles)
      AND r.deleted = FALSE;

    -- 2. Remove user_roles assignments for surplus roles
    DELETE FROM user_roles ur
    USING roles r
    WHERE ur.role_id = r.id
      AND r.tenant_id = ANY(affected_tenants)
      AND r.name = ANY(surplus_roles)
      AND r.deleted = FALSE;

    -- 3. Soft-delete the surplus role rows
    UPDATE roles
    SET deleted    = TRUE,
        deleted_at = NOW()
    WHERE tenant_id = ANY(affected_tenants)
      AND name = ANY(surplus_roles)
      AND deleted = FALSE;

    RAISE NOTICE 'Done. Surplus roles removed from PAWN_SHOP and JEWELRY tenants.';
END $$;
