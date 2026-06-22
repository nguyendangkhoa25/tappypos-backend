-- Stale-token signal for instant subscription/feature propagation.
--
-- Each access token carries an `fv` (features version) claim equal to the tenant's
-- features_version at issue time. When the master admin changes a tenant's features,
-- TenantService bumps features_version; the next request from any device carries a
-- now-stale fv, TenantInterceptor returns 401 TOKEN_STALE, and the client silently
-- refreshes (no logout) to pick up the new features within seconds.
--
-- `tenants` is a master table (no RLS).

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS features_version INT NOT NULL DEFAULT 0;
