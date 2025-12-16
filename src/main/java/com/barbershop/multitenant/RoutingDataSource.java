package com.barbershop.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * DynamicTenantRoutingDataSource - Dynamically creates and routes to tenant databases
 * <p>
 * This class extends AbstractRoutingDataSource to provide dynamic tenant database routing.
 * It creates DataSource connections on-demand when a tenant is first accessed and caches
 * them for subsequent requests.
 * <p>
 * Features:
 * - Dynamic DataSource creation per tenant
 * - Connection pooling per tenant database
 * - Automatic fallback to master database
 * - Thread-safe DataSource caching
 */
@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    private final TenantContext tenantContext;

    public RoutingDataSource(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
        log.info("DynamicTenantRoutingDataSource created with TenantContext: {}", tenantContext != null ? "SUCCESS" : "NULL");
    }

    /**
     * Determine which DataSource to use based on current tenant
     * Called before each database operation
     *
     * @return tenant ID (used as key to lookup DataSource)
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = tenantContext.getCurrentTenantId();

        log.info("DynamicTenantRoutingDataSource.determineCurrentLookupKey() called - tenantId: {}", tenantId);

        if (tenantId != null) {
            log.info("Routing database query to tenant: {}", tenantId);
            return tenantId;
        }

        // If no tenant context, use default/master database
        log.info("No tenant context found, using master DataSource");
        return "master";
    }
}
