package com.barbershop.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * RoutingDataSource - Dynamically creates and routes to tenant databases
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
 * - Add/remove tenant datasources at runtime without app restart
 */
@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    private final TenantContext tenantContext;

    public RoutingDataSource(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * Determine which DataSource to use based on current tenant
     * Called before each database operation
     *
     * @return tenant ID (used as key to lookup DataSource) or "master" for master database
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = tenantContext.getCurrentTenantId();
        log.debug("RoutingDataSource.determineCurrentLookupKey() called - tenantId: {}", tenantId);
        if (tenantId != null) {
            log.info("Routing database query to tenant: {}", tenantId);
            return tenantId;
        }

        // If no tenant context, use default/master database
        log.info("No tenant context found, using master DataSource");
        return "master";
    }

    /**
     * Add or update a tenant datasource at runtime
     * Thread-safe operation that updates the target datasources map
     *
     * @param tenantId   the tenant ID key
     * @param dataSource the datasource to add/update
     */
    public synchronized void addTargetDataSource(String tenantId, DataSource dataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>(getResolvedDataSources());
        targetDataSources.put(tenantId, dataSource);
        setTargetDataSources(targetDataSources);
        // CRITICAL: Reinitialize resolver cache after updating datasources
        try {
            afterPropertiesSet();
        } catch (Exception e) {
            log.error("Error reinitializing datasource resolver", e);
        }
        log.info("Added/updated datasource for tenant: {}", tenantId);
    }

    /**
     * Remove a tenant datasource at runtime
     * Thread-safe operation that updates the target datasources map
     *
     * @param tenantId the tenant ID key to remove
     */
    public synchronized void removeTargetDataSource(String tenantId) {
        Map<Object, Object> targetDataSources = new HashMap<>(getResolvedDataSources());
        targetDataSources.remove(tenantId);
        setTargetDataSources(targetDataSources);
        // CRITICAL: Reinitialize resolver cache after updating datasources
        try {
            afterPropertiesSet();
        } catch (Exception e) {
            log.error("Error reinitializing datasource resolver", e);
        }

        log.info("Removed datasource for tenant: {}", tenantId);
    }
}

