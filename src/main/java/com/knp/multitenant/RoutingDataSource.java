package com.knp.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RoutingDataSource - Routes DB connections to the correct tenant database.
 *
 * Uses an internal ConcurrentHashMap (tenantDataSources) as the source of truth
 * for active datasources. This bypasses AbstractRoutingDataSource.resolvedDataSources,
 * which is unreliable to update at runtime because afterPropertiesSet() replaces the
 * entire map and any exception inside it silently leaves routing broken.
 *
 * add/removeTargetDataSource mutate the internal map directly and take effect
 * immediately on the next request — no afterPropertiesSet() call needed.
 */
@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    private final TenantContext tenantContext;
    private final ConcurrentHashMap<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();

    public RoutingDataSource(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        // Seed our internal map from whatever DatasourceConfig loaded at startup
        getResolvedDataSources().forEach((k, v) -> tenantDataSources.put(k.toString(), v));
        log.info("RoutingDataSource initialized with {} datasource(s): {}",
                tenantDataSources.size(), tenantDataSources.keySet());
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = tenantContext.getCurrentTenantId();
        if (tenantId != null) {
            log.debug("Routing to tenant: {}", tenantId);
            return tenantId;
        }
        log.debug("No tenant context, routing to master");
        return "master";
    }

    /**
     * Overridden to use our own map instead of resolvedDataSources.
     * Falls back to master when the key is not registered.
     */
    @Override
    protected DataSource determineTargetDataSource() {
        String key = (String) determineCurrentLookupKey();
        DataSource ds = tenantDataSources.get(key);
        if (ds != null) {
            return ds;
        }
        log.warn("No datasource registered for key '{}'. Registered keys: {}. Falling back to master", key, tenantDataSources.keySet());
        DataSource master = tenantDataSources.get("master");
        if (master != null) {
            return master;
        }
        return super.determineTargetDataSource();
    }

    /**
     * Register or replace a tenant datasource.
     * Takes effect immediately on the next DB operation — no restart required.
     */
    public void addTargetDataSource(String tenantId, DataSource dataSource) {
        if (dataSource == null) {
            log.warn("Ignoring null datasource for tenant: {}", tenantId);
            return;
        }
        tenantDataSources.put(tenantId, dataSource);
        log.info("Registered datasource for tenant: {}. Active keys: {}", tenantId, tenantDataSources.keySet());
    }

    /**
     * Remove a tenant datasource.
     * Takes effect immediately on the next DB operation.
     */
    public void removeTargetDataSource(String tenantId) {
        tenantDataSources.remove(tenantId);
        log.info("Removed datasource for tenant: {}", tenantId);
    }

    /**
     * Full reload — replaces all entries (master + tenants) atomically.
     * Called by DatasourceManager.reloadAllTenantDatasource().
     */
    public void reloadTargetDataSources(Map<String, DataSource> allDataSources) {
        tenantDataSources.clear();
        tenantDataSources.putAll(allDataSources);
        log.info("Reloaded datasources, total registered: {}", tenantDataSources.size());
    }
}
