-- V002: Exclude soft-deleted users from username uniqueness constraints.
--
-- Problem: deleteUser() in UserService is a soft delete (sets deleted=true).
-- The old constraint uq_users_username_tenant had no "deleted" filter, so
-- attempting to re-create a user with the same username after a soft-delete
-- would throw DataIntegrityViolationException (409) even though the application-
-- level existsByUsernameTenantScoped() check (which filters deleted <> true)
-- had already passed. This manifested as a persistent 409 in E2E test HAIR-06.
--
-- Fix: Rebuild both username unique indexes as partial indexes that exclude
-- soft-deleted rows (deleted <> true). This makes them consistent with the
-- application-level checks in UserRepository.

DROP INDEX IF EXISTS uq_users_username_tenant;
CREATE UNIQUE INDEX uq_users_username_tenant
    ON users(username, tenant_id)
    WHERE tenant_id IS NOT NULL AND deleted <> true;

DROP INDEX IF EXISTS uq_users_username_master;
CREATE UNIQUE INDEX uq_users_username_master
    ON users(username)
    WHERE tenant_id IS NULL AND deleted <> true;
