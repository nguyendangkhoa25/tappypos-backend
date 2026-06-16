package com.tappy.pos.service.tenant;

import java.util.Set;

/**
 * Tenant-related constants.
 *
 * <p>{@link #RESERVED_TENANT_IDS} are URL path segments that must never be used
 * as a tenant ID, because the frontend routes them to something other than a
 * tenant: app-level public routes ({@code login}, {@code maintenance}, …), the
 * {@code master} super-admin context, and every per-shop-type marketing landing
 * page slug (e.g. {@code pos-quan-cafe}).
 *
 * <p>This list mirrors the frontend: {@code RESERVED_PATHS} in
 * {@code src/providers/TenantProvider.jsx} plus {@code SHOP_LANDING_SLUGS} from
 * {@code src/pages/landings/content/index.js}. Keep the two in sync when adding
 * or removing a marketing landing page. All entries are lowercase; comparison is
 * case-insensitive.
 */
public final class TenantConstants {

    private TenantConstants() {
    }

    public static final Set<String> RESERVED_TENANT_IDS = Set.of(
            // App-level reserved routes / contexts
            "master",
            "login",
            "master-login",
            "maintenance",
            "access-denied",
            "tenant-expired",
            "tenant-not-found",
            "register",
            "onboarding",
            // Per-shop-type marketing landing page slugs
            "pos-quan-cafe",
            "pos-nha-hang",
            "quan-ly-spa",
            "pos-tiem-nail",
            "pos-tiem-toc",
            "pos-tiem-vang",
            "pos-cam-do",
            "pos-tap-hoa",
            "pos-nha-thuoc",
            "pos-thoi-trang",
            "quan-ly-bida",
            "quan-ly-san-tennis"
    );

    /**
     * @return true when the given tenant ID collides with a reserved route/slug
     * (case-insensitive, null-safe).
     */
    public static boolean isReserved(String tenantId) {
        return tenantId != null && RESERVED_TENANT_IDS.contains(tenantId.trim().toLowerCase());
    }
}
